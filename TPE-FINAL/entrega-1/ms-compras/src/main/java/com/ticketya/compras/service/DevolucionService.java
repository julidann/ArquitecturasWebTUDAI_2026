package com.ticketya.compras.service;

import com.ticketya.compras.client.PagosClient;
import com.ticketya.compras.client.ReintegroDto;
import com.ticketya.compras.client.ReservasClient;
import com.ticketya.compras.dto.SolicitudDevolucionResponse;
import com.ticketya.compras.exception.*;
import com.ticketya.compras.model.*;
import com.ticketya.compras.repository.EntradaRepository;
import com.ticketya.compras.repository.SolicitudDevolucionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Devoluciones (funcionalidad 7): el cliente pide la devolucion hasta 72 hs antes del
 * evento y un administrador la aprueba o rechaza. Al aprobar se libera el lugar en
 * ms-reservas y se pide el reintegro a ms-pagos; las dos llamadas usan la clave
 * "devolucion-{id}" como Idempotency-Key, asi si algo falla a mitad de camino el admin
 * puede volver a aprobar sin liberar ni reintegrar dos veces.
 */
@Service
public class DevolucionService {

    private static final Logger log = LoggerFactory.getLogger(DevolucionService.class);

    private final EntradaRepository entradaRepository;
    private final SolicitudDevolucionRepository solicitudRepository;
    private final ReservasClient reservasClient;
    private final PagosClient pagosClient;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final int horasMinimas;

    public DevolucionService(EntradaRepository entradaRepository, SolicitudDevolucionRepository solicitudRepository,
                             ReservasClient reservasClient, PagosClient pagosClient, TransactionTemplate tx,
                             Clock clock, @Value("${devoluciones.horas-minimas:72}") int horasMinimas) {
        this.entradaRepository = entradaRepository;
        this.solicitudRepository = solicitudRepository;
        this.reservasClient = reservasClient;
        this.pagosClient = pagosClient;
        this.tx = tx;
        this.clock = clock;
        this.horasMinimas = horasMinimas;
    }

    public SolicitudDevolucionResponse solicitar(Long entradaId, Long usuarioId, String motivo) {
        return tx.execute(s -> {
            Entrada entrada = entradaRepository.findById(entradaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe la entrada " + entradaId));
            if (!entrada.getUsuarioId().equals(usuarioId)) {
                throw new AccesoDenegadoException("La entrada no pertenece al usuario");
            }
            if (entrada.getEstado() != EstadoEntrada.VALIDA) {
                throw new OperacionInvalidaException("La entrada está " + entrada.getEstado()
                        + ", no se puede devolver");
            }
            LocalDateTime ahora = LocalDateTime.now(clock);
            LocalDateTime limite = entrada.getCompra().getFechaEvento().minusHours(horasMinimas);
            if (ahora.isAfter(limite)) {
                throw new ReglaNegocioException("La devolución solo se puede pedir hasta " + horasMinimas
                        + " horas antes del evento (límite: " + limite + ")");
            }
            if (solicitudRepository.existsByEntradaIdAndEstado(entradaId, EstadoSolicitud.PENDIENTE)) {
                throw new OperacionInvalidaException("Ya hay una solicitud de devolución pendiente para esta entrada");
            }
            SolicitudDevolucion sol = solicitudRepository.save(new SolicitudDevolucion(entrada, usuarioId, motivo, ahora));
            log.info("Solicitud de devolución {} creada para la entrada {}", sol.getId(), entradaId);
            return SolicitudDevolucionResponse.de(sol);
        });
    }

    public SolicitudDevolucionResponse aprobar(Long solicitudId, Long adminId, Rol rol) {
        exigirAdmin(rol);
        SolicitudDevolucion sol = tx.execute(s -> {
            SolicitudDevolucion x = buscar(solicitudId);
            x.getEntrada().getCompra().getPagoId(); // inicializa la compra (lazy)
            return x;
        });
        if (sol.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new OperacionInvalidaException("La solicitud ya fue " + sol.getEstado());
        }
        Entrada entrada = sol.getEntrada();
        String clave = "devolucion-" + solicitudId;

        // 1) liberar el lugar  2) reintegrar el dinero. Si falla alguno no se toca nada local.
        reservasClient.liberarVendido(entrada.getEventoId(), entrada.getCompra().getReservaId(),
                entrada.getSectorId(), entrada.getAsientoId(), clave);
        ReintegroDto reintegro = pagosClient.reintegrar(entrada.getCompra().getPagoId(), entrada.getPrecio(),
                "Devolución de la entrada " + entrada.getId(), clave);

        return tx.execute(s -> {
            SolicitudDevolucion x = buscar(solicitudId);
            x.getEntrada().devolver();
            x.aprobar(adminId, reintegro.id(), LocalDateTime.now(clock));
            log.info("Devolución {} aprobada por el admin {}: entrada {} devuelta, reintegro {}", solicitudId,
                    adminId, x.getEntrada().getId(), reintegro.id());
            return SolicitudDevolucionResponse.de(x);
        });
    }

    public SolicitudDevolucionResponse rechazar(Long solicitudId, Long adminId, Rol rol, String observacion) {
        exigirAdmin(rol);
        return tx.execute(s -> {
            SolicitudDevolucion sol = buscar(solicitudId);
            if (sol.getEstado() != EstadoSolicitud.PENDIENTE) {
                throw new OperacionInvalidaException("La solicitud ya fue " + sol.getEstado());
            }
            sol.rechazar(adminId, observacion, LocalDateTime.now(clock));
            log.info("Devolución {} rechazada por el admin {}", solicitudId, adminId);
            return SolicitudDevolucionResponse.de(sol);
        });
    }

    /** El admin ve todas (opcionalmente filtradas por estado); el resto solo las propias. */
    public List<SolicitudDevolucionResponse> listar(Long usuarioId, Rol rol, EstadoSolicitud estado) {
        return tx.execute(s -> {
            List<SolicitudDevolucion> lista;
            if (rol == Rol.ADMIN) {
                lista = estado == null ? solicitudRepository.findAllByOrderBySolicitadaEnDesc()
                        : solicitudRepository.findByEstadoOrderBySolicitadaEn(estado);
            } else {
                lista = solicitudRepository.findByUsuarioIdOrderBySolicitadaEnDesc(usuarioId).stream()
                        .filter(x -> estado == null || x.getEstado() == estado)
                        .toList();
            }
            return lista.stream().map(SolicitudDevolucionResponse::de).toList();
        });
    }

    private SolicitudDevolucion buscar(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la solicitud de devolución " + id));
    }

    private void exigirAdmin(Rol rol) {
        if (rol != Rol.ADMIN) {
            throw new AccesoDenegadoException("Solo un administrador puede resolver devoluciones");
        }
    }
}
