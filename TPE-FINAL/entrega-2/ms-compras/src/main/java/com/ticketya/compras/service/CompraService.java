package com.ticketya.compras.service;

import com.ticketya.compras.client.PagoDto;
import com.ticketya.compras.client.PagosClient;
import com.ticketya.compras.client.ReservaDto;
import com.ticketya.compras.client.ReservasClient;
import com.ticketya.compras.dto.CompraRequest;
import com.ticketya.compras.dto.CompraResponse;
import com.ticketya.compras.dto.DatosMedioPago;
import com.ticketya.compras.exception.*;
import com.ticketya.compras.mensajeria.Mensajes;
import com.ticketya.compras.mensajeria.Outbox;
import com.ticketya.compras.model.Compra;
import com.ticketya.compras.model.Entrada;
import com.ticketya.compras.model.EstadoCompra;
import com.ticketya.compras.model.MedioPago;
import com.ticketya.compras.repository.CompraRepository;
import com.ticketya.compras.trazabilidad.Bitacora;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Flujo de compra (funcionalidad 4), implementado como una SAGA ORQUESTADA desde ms-compras:
 *
 * 1. Se valida la reserva contra ms-reservas (que sea del cliente y siga pendiente).
 * 2. Se registra la compra en PENDIENTE con la Idempotency-Key del cliente.
 * 3. Se cobra en ms-pagos (con la clave "compra-{id}", asi un reintento no cobra dos veces).
 * 4a. Pago aprobado: se confirma la reserva y se emiten las entradas con QR (+ mensaje compra.confirmada).
 * 4b. Pago rechazado: se libera la reserva (compensacion del paso 1).
 * 4c. Pago aprobado pero la reserva ya vencio: se reintegra el dinero (compensacion del paso 3).
 * 4d. Falla de comunicacion a mitad de camino: la compra queda en ERROR y la retoman el cliente
 *     (reintento con la misma clave) o {@link RecuperacionComprasJob}; nunca queda un cobro sin entradas.
 *
 * Las llamadas remotas NO se hacen dentro de una transaccion de base de datos (no
 * queremos tener una conexion tomada mientras esperamos a la pasarela). Cada cambio de
 * estado local se guarda en su propia transaccion corta.
 */
@Service
public class CompraService {

    private static final Logger log = LoggerFactory.getLogger(CompraService.class);

    private final CompraRepository compraRepository;
    private final ReservasClient reservasClient;
    private final PagosClient pagosClient;
    private final TransactionTemplate tx;
    private final Outbox outbox;
    private final Bitacora bitacora;

    public CompraService(CompraRepository compraRepository, ReservasClient reservasClient, PagosClient pagosClient,
                         TransactionTemplate tx, Outbox outbox, Bitacora bitacora) {
        this.compraRepository = compraRepository;
        this.reservasClient = reservasClient;
        this.pagosClient = pagosClient;
        this.tx = tx;
        this.outbox = outbox;
        this.bitacora = bitacora;
    }

    public ResultadoCompra comprar(Long usuarioId, CompraRequest req, String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("Falta el encabezado Idempotency-Key");
        }
        validarMedioPago(req.medioPago());

        Compra existente = compraRepository.findByIdempotencyKey(clave).orElse(null);
        if (existente != null) {
            return reintento(existente, usuarioId, req);
        }

        ReservaDto reserva = reservasClient.obtener(req.reservaId());
        if (!reserva.usuarioId().equals(usuarioId)) {
            throw new AccesoDenegadoException("La reserva no pertenece al usuario");
        }
        if (!reserva.estaPendiente()) {
            throw new OperacionInvalidaException("La reserva " + reserva.id() + " no está vigente (estado "
                    + reserva.estado() + (reserva.segundosRestantes() <= 0 ? ", vencida" : "") + ")");
        }

        Compra compra;
        try {
            compra = tx.execute(s -> compraRepository.saveAndFlush(new Compra(clave, usuarioId, reserva.id(),
                    reserva.eventoId(), reserva.nombreEvento(), reserva.fechaEvento(), reserva.sectorId(),
                    reserva.sector(), reserva.cantidad(), reserva.montoTotal(), req.medioPago().tipo(),
                    req.medioPago().ultimosDigitos())));
        } catch (DataIntegrityViolationException e) {
            // o llego otro pedido con la misma clave al mismo tiempo, o ya hay una compra para la reserva
            Compra porClave = compraRepository.findByIdempotencyKey(clave).orElse(null);
            if (porClave != null) {
                return reintento(porClave, usuarioId, req);
            }
            Compra porReserva = compraRepository.findByReservaId(req.reservaId()).orElseThrow(() -> e);
            throw new OperacionInvalidaException("Ya existe la compra " + porReserva.getId() + " para la reserva "
                    + req.reservaId() + ". Si querés reintentarla usá la misma Idempotency-Key.");
        }
        log.info("Compra {} iniciada: usuario={}, reserva={}, monto={}", compra.getId(), usuarioId, reserva.id(),
                compra.getMontoTotal());
        return procesar(compra, req.medioPago());
    }

    /** Mismo pedido repetido por el cliente. */
    private ResultadoCompra reintento(Compra compra, Long usuarioId, CompraRequest req) {
        if (!compra.getUsuarioId().equals(usuarioId) || !compra.getReservaId().equals(req.reservaId())) {
            throw new ReglaNegocioException("La Idempotency-Key ya se usó para otra compra");
        }
        return switch (compra.getEstado()) {
            case APROBADA, RECHAZADA -> {
                log.info("Reintento de la compra {} con la misma clave: se devuelve el resultado original", compra.getId());
                yield new ResultadoCompra(respuesta(compra.getId()), HttpStatus.OK);
            }
            case PENDIENTE -> throw new OperacionInvalidaException("La compra " + compra.getId()
                    + " se está procesando, consultá su estado en unos segundos");
            case ERROR -> {
                if (!reclamar(compra.getId())) {
                    throw new OperacionInvalidaException("La compra " + compra.getId()
                            + " se está procesando, consultá su estado en unos segundos");
                }
                log.info("Reintentando la compra {} que había quedado en ERROR", compra.getId());
                yield procesar(compra, req.medioPago());
            }
        };
    }

    private ResultadoCompra procesar(Compra compra, DatosMedioPago medio) {
        Long id = compra.getId();

        PagoDto pago;
        try {
            pago = pagosClient.pagar(id, compra.getUsuarioId(), compra.getMontoTotal(), medio, "compra-" + id);
        } catch (ServicioNoDisponibleException e) {
            return error(id, "No se pudo contactar a la pasarela de pagos. Reintentá con la misma Idempotency-Key.");
        }
        return continuarConPago(compra, pago);
    }

    /**
     * Segunda mitad de la saga, una vez que se conoce el resultado del cobro. La usa tambien la
     * recuperacion automatica (que no tiene los datos de la tarjeta, pero si el pago ya registrado).
     */
    ResultadoCompra continuarConPago(Compra compra, PagoDto pago) {
        Long id = compra.getId();
        if (!pago.aprobado()) {
            liberarReserva(compra.getReservaId());
            guardar(id, c -> c.rechazar(pago.id(), "Pago rechazado: " + pago.motivoRechazo()));
            log.info("Compra {} rechazada: {}", id, pago.motivoRechazo());
            registrar("compra.rechazada", compra, Map.of("motivo", String.valueOf(pago.motivoRechazo())));
            return new ResultadoCompra(respuesta(id), HttpStatus.PAYMENT_REQUIRED);
        }
        guardar(id, c -> c.registrarPago(pago.id()));

        ReservaDto confirmada;
        try {
            confirmada = reservasClient.confirmar(compra.getReservaId());
        } catch (RespuestaServicioException e) {
            // la reserva vencio (o el evento se cancelo) mientras se procesaba el pago: compensamos
            return compensar(compra, pago, e.getMessage());
        } catch (ServicioNoDisponibleException e) {
            return error(id, "Pago aprobado pero no se pudo confirmar la reserva. Reintentá con la misma Idempotency-Key.");
        }

        tx.executeWithoutResult(s -> {
            Compra c = compraRepository.findById(id).orElseThrow();
            for (ReservaDto.Item item : confirmada.items()) {
                c.agregarEntrada(new Entrada(c.getUsuarioId(), c.getEventoId(), c.getSectorId(), item.asientoId(),
                        item.fila(), item.numero(), confirmada.precioUnitario()));
            }
            c.aprobar(pago.id());
            // en la misma transaccion: si se emiten las entradas, el mensaje sale seguro (outbox)
            outbox.registrar(Mensajes.COMPRA_CONFIRMADA, new Mensajes.CompraConfirmada(c.getId(), c.getUsuarioId(),
                    c.getEventoId(), c.getNombreEvento(), c.getFechaEvento(), c.getSectorNombre(), c.getCantidad(),
                    c.getMontoTotal()));
        });
        log.info("Compra {} aprobada: {} entradas emitidas", id, confirmada.items().size());
        registrar("compra.aprobada", compra, Map.of("pagoId", pago.id(), "entradas", confirmada.items().size()));
        return new ResultadoCompra(respuesta(id), HttpStatus.CREATED);
    }

    ResultadoCompra compensar(Compra compra, PagoDto pago, String motivo) {
        Long id = compra.getId();
        log.warn("Compra {}: pago {} aprobado pero la reserva no se pudo confirmar ({}). Se reintegra el dinero.",
                id, pago.id(), motivo);
        try {
            pagosClient.reintegrar(pago.id(), compra.getMontoTotal(), "Compensación compra " + id + ": " + motivo,
                    "compensacion-compra-" + id);
        } catch (RuntimeException e) {
            return error(id, "La reserva no se pudo confirmar y falló el reintegro automático; "
                    + "reintentá con la misma Idempotency-Key.");
        }
        guardar(id, c -> c.rechazar(pago.id(), "No se pudo confirmar la reserva (" + motivo + "). Se reintegró el pago."));
        registrar("compra.compensada", compra, Map.of("pagoId", pago.id(), "motivo", String.valueOf(motivo)));
        return new ResultadoCompra(respuesta(id), HttpStatus.CONFLICT);
    }

    /** Se cerro la compra sin cobro (lo decide la recuperacion: la pasarela no registra ningun pago). */
    void cerrarSinCobro(Compra compra, String motivo) {
        liberarReserva(compra.getReservaId());
        guardar(compra.getId(), c -> c.rechazar(null, motivo));
        registrar("compra.rechazada", compra, Map.of("motivo", motivo));
    }

    boolean reclamar(Long id) {
        Integer n = tx.execute(s -> compraRepository.reclamarParaReintento(id, LocalDateTime.now()));
        return n != null && n == 1;
    }

    private void liberarReserva(Long reservaId) {
        try {
            reservasClient.liberar(reservaId);
        } catch (RuntimeException e) {
            // no es grave: si no se pudo liberar ahora, el job de vencimiento la libera igual
            log.warn("No se pudo liberar la reserva {}: {}", reservaId, e.getMessage());
        }
    }

    ResultadoCompra error(Long id, String motivo) {
        guardar(id, c -> c.marcarError(motivo));
        log.error("Compra {} en ERROR: {}", id, motivo);
        Compra compra = compraRepository.findById(id).orElseThrow();
        registrar("compra.error", compra, Map.of("motivo", motivo));
        return new ResultadoCompra(respuesta(id), HttpStatus.SERVICE_UNAVAILABLE);
    }

    private void guardar(Long id, Consumer<Compra> cambio) {
        tx.executeWithoutResult(s -> cambio.accept(compraRepository.findById(id).orElseThrow()));
    }

    private void registrar(String operacion, Compra compra, Map<String, Object> extra) {
        Map<String, Object> detalle = new HashMap<>(extra);
        detalle.put("compraId", compra.getId());
        detalle.put("reservaId", compra.getReservaId());
        detalle.put("eventoId", compra.getEventoId());
        detalle.put("monto", compra.getMontoTotal());
        String resultado = switch (operacion) {
            case "compra.aprobada" -> "OK";
            case "compra.error" -> "ERROR";
            default -> "RECHAZADA";
        };
        bitacora.registrar(operacion, resultado, compra.getUsuarioId(), detalle);
    }

    private void validarMedioPago(DatosMedioPago medio) {
        if (medio.tipo() != MedioPago.MERCADO_PAGO && medio.numeroTarjeta() == null) {
            throw new IllegalArgumentException("Para pagar con tarjeta hay que indicar numeroTarjeta");
        }
    }

    public CompraResponse obtener(Long id, Long usuarioId, String rol) {
        CompraResponse c = respuesta(id);
        if (!"ADMIN".equals(rol) && usuarioId != null && !c.usuarioId().equals(usuarioId)) {
            throw new AccesoDenegadoException("La compra no pertenece al usuario");
        }
        return c;
    }

    public CompraResponse obtener(Long id, Long usuarioId) {
        return obtener(id, usuarioId, null);
    }

    public List<CompraResponse> deUsuario(Long usuarioId) {
        return tx.execute(s -> compraRepository.findByUsuarioIdOrderByCreadaEnDesc(usuarioId).stream()
                .map(CompraResponse::de).toList());
    }

    CompraResponse respuesta(Long id) {
        return tx.execute(s -> CompraResponse.de(compraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la compra " + id))));
    }

    long comprasEnEstado(EstadoCompra estado) {
        return compraRepository.countByEstado(estado);
    }
}
