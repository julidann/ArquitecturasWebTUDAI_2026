package com.ticketya.reservas.service;

import com.ticketya.reservas.client.EventosClient;
import com.ticketya.reservas.dto.LiberacionRequest;
import com.ticketya.reservas.dto.ReservaRequest;
import com.ticketya.reservas.dto.ReservaResponse;
import com.ticketya.reservas.exception.*;
import com.ticketya.reservas.model.*;
import com.ticketya.reservas.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Reservas temporales de lugares (funcionalidad 4) y control de doble venta (funcionalidad 5).
 *
 * Todas las transiciones de estado (de asientos, cupos y reservas) se hacen con UPDATEs
 * condicionales sobre la base de datos, que es el unico punto compartido entre varias
 * instancias de este servicio. No hay locks en memoria (synchronized) porque no
 * servirian con mas de una instancia.
 */
@Service
public class ReservaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);

    private final ReservaRepository reservaRepository;
    private final EventoDisponibilidadRepository eventoRepository;
    private final SectorDisponibilidadRepository sectorRepository;
    private final AsientoDisponibilidadRepository asientoRepository;
    private final LiberacionRepository liberacionRepository;
    private final EventosClient eventosClient;
    private final Clock clock;
    private final int minutosVigencia;

    public ReservaService(ReservaRepository reservaRepository, EventoDisponibilidadRepository eventoRepository,
                          SectorDisponibilidadRepository sectorRepository,
                          AsientoDisponibilidadRepository asientoRepository,
                          LiberacionRepository liberacionRepository, EventosClient eventosClient, Clock clock,
                          @Value("${reservas.minutos-vigencia:10}") int minutosVigencia) {
        this.reservaRepository = reservaRepository;
        this.eventoRepository = eventoRepository;
        this.sectorRepository = sectorRepository;
        this.asientoRepository = asientoRepository;
        this.liberacionRepository = liberacionRepository;
        this.eventosClient = eventosClient;
        this.clock = clock;
        this.minutosVigencia = minutosVigencia;
    }

    @Transactional
    public ReservaResponse crear(Long usuarioId, ReservaRequest req) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        EventoDisponibilidad evento = eventoRepository.findById(req.eventoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El evento " + req.eventoId()
                        + " no existe o no está publicado"));
        if (!evento.isHabilitado()) {
            throw new OperacionInvalidaException("El evento no tiene la venta habilitada");
        }
        if (!evento.getFechaEvento().isAfter(ahora)) {
            throw new OperacionInvalidaException("El evento ya ocurrió");
        }
        SectorDisponibilidad sector = sectorRepository.findByEventoIdAndSectorId(req.eventoId(), req.sectorId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El evento no tiene el sector " + req.sectorId()));

        boolean numerado = sector.getTipo() == TipoSector.NUMERADO;
        int cantidad = validarCantidad(req, numerado);

        long yaTomadas = reservaRepository.cantidadTomadaPorCliente(usuarioId, req.eventoId());
        if (yaTomadas + cantidad > evento.getLimitePorCliente()) {
            throw new LimiteExcedidoException("Se permite un máximo de " + evento.getLimitePorCliente()
                    + " entradas por cliente para este evento (ya tenés " + yaTomadas + ")");
        }

        Reserva reserva = new Reserva(usuarioId, req.eventoId(), sector, cantidad, ahora, minutosVigencia);
        if (numerado) {
            List<AsientoDisponibilidad> asientos = asientoRepository.findByEventoIdAndAsientoIdIn(req.eventoId(),
                    req.asientoIds());
            if (asientos.size() != cantidad || asientos.stream().anyMatch(a -> !a.getSectorId().equals(req.sectorId()))) {
                throw new RecursoNoEncontradoException("Alguno de los asientos no pertenece al sector indicado");
            }
            asientos.stream()
                    .sorted(Comparator.comparing(AsientoDisponibilidad::getFila).thenComparing(AsientoDisponibilidad::getNumero))
                    .forEach(a -> reserva.agregarItem(new ReservaItem(a.getAsientoId(), a.getFila(), a.getNumero())));
        } else {
            for (int i = 0; i < cantidad; i++) {
                reserva.agregarItem(new ReservaItem(null, null, null));
            }
        }
        reservaRepository.saveAndFlush(reserva);

        if (numerado) {
            int tomados = asientoRepository.reservar(req.eventoId(), req.sectorId(), req.asientoIds(), reserva.getId());
            if (tomados != cantidad) {
                // rollback de toda la transaccion: ni la reserva ni los asientos que si se tomaron quedan
                throw new LugarNoDisponibleException("Alguno de los asientos elegidos ya no está disponible");
            }
        }
        if (sectorRepository.descontar(req.eventoId(), req.sectorId(), cantidad) == 0) {
            throw new LugarNoDisponibleException(numerado
                    ? "Alguno de los asientos elegidos ya no está disponible"
                    : "No quedan lugares suficientes en " + sector.getNombre());
        }
        log.info("Reserva {} creada: usuario={}, evento={}, sector={}, cantidad={}, vence={}", reserva.getId(),
                usuarioId, req.eventoId(), req.sectorId(), cantidad, reserva.getExpiraEn());
        return ReservaResponse.de(reserva, evento.getNombreEvento(), evento.getFechaEvento(), sector.getNombre());
    }

    private int validarCantidad(ReservaRequest req, boolean numerado) {
        if (numerado) {
            if (req.asientoIds() == null || req.asientoIds().isEmpty()) {
                throw new IllegalArgumentException("Para un sector numerado hay que indicar los asientos (asientoIds)");
            }
            if (new HashSet<>(req.asientoIds()).size() != req.asientoIds().size()) {
                throw new IllegalArgumentException("Hay asientos repetidos en el pedido");
            }
            return req.asientoIds().size();
        }
        if (req.asientoIds() != null && !req.asientoIds().isEmpty()) {
            throw new IllegalArgumentException("El sector no es numerado: no se pueden elegir asientos");
        }
        if (req.cantidad() == null || req.cantidad() < 1) {
            throw new IllegalArgumentException("Para un sector no numerado hay que indicar la cantidad");
        }
        return req.cantidad();
    }

    @Transactional(readOnly = true)
    public ReservaResponse obtener(Long id) {
        return armarRespuesta(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> deUsuario(Long usuarioId, EstadoReserva estado) {
        List<Reserva> reservas = estado == null
                ? reservaRepository.findByUsuarioIdOrderByCreadaEnDesc(usuarioId)
                : reservaRepository.findByUsuarioIdAndEstadoOrderByCreadaEnDesc(usuarioId, estado);
        return reservas.stream().map(this::armarRespuesta).toList();
    }

    /**
     * La invoca ms-compras cuando el pago fue aprobado. Es idempotente: si la reserva ya
     * estaba confirmada devuelve OK, asi un reintento de ms-compras no falla.
     */
    @Transactional
    public ReservaResponse confirmar(Long id) {
        Reserva reserva = buscar(id);
        if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            return armarRespuesta(reserva);
        }
        EventoDisponibilidad evento = eventoRepository.findById(reserva.getEventoId()).orElseThrow();
        if (!evento.isHabilitado()) {
            throw new OperacionInvalidaException("El evento ya no tiene la venta habilitada");
        }
        if (reservaRepository.confirmarSiVigente(id, LocalDateTime.now(clock)) == 0) {
            Reserva actual = buscar(id);
            throw new OperacionInvalidaException("La reserva " + id + " no se puede confirmar: está "
                    + (actual.estaVencida(LocalDateTime.now(clock)) ? "VENCIDA" : actual.getEstado()));
        }
        if (reserva.getTipoSector() == TipoSector.NUMERADO) {
            asientoRepository.venderDeReserva(id);
        }
        Long eventoId = reserva.getEventoId();
        despuesDelCommit(() -> {
            if (sectorRepository.totalDisponibles(eventoId) == 0) {
                eventosClient.marcarAgotado(eventoId);
            }
        });
        log.info("Reserva {} confirmada", id);
        return armarRespuesta(buscar(id));
    }

    /**
     * Libera los lugares de una reserva pendiente (pago rechazado, vencimiento o cancelacion).
     * Solo la primera llamada tiene efecto gracias al UPDATE condicional sobre el estado.
     */
    @Transactional
    public ReservaResponse liberar(Long id, EstadoReserva estadoFinal) {
        Reserva reserva = buscar(id);
        int cambio = reservaRepository.finalizarSiPendiente(id, estadoFinal, LocalDateTime.now(clock));
        if (cambio == 0) {
            Reserva actual = buscar(id);
            if (actual.getEstado() == EstadoReserva.CONFIRMADA) {
                throw new OperacionInvalidaException("La reserva " + id + " ya fue confirmada, no se puede liberar");
            }
            return armarRespuesta(actual); // ya estaba liberada o vencida
        }
        if (reserva.getTipoSector() == TipoSector.NUMERADO) {
            asientoRepository.liberarDeReserva(id);
        }
        sectorRepository.reponer(reserva.getEventoId(), reserva.getSectorId(), reserva.getCantidad());
        log.info("Reserva {} pasó a {} y se liberaron {} lugares", id, estadoFinal, reserva.getCantidad());
        return armarRespuesta(buscar(id));
    }

    @Transactional
    public ReservaResponse cancelarPorCliente(Long id, Long usuarioId) {
        Reserva reserva = buscar(id);
        if (!reserva.getUsuarioId().equals(usuarioId)) {
            throw new AccesoDenegadoException("La reserva no pertenece al usuario");
        }
        return liberar(id, EstadoReserva.LIBERADA);
    }

    /**
     * Devuelve al inventario un lugar ya vendido (devolucion aprobada). Idempotente por clave.
     */
    @Transactional
    public void liberarVendido(Long eventoId, LiberacionRequest req, String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("Falta el encabezado Idempotency-Key");
        }
        if (liberacionRepository.existsByClave(clave)) {
            log.info("Liberacion {} ya procesada, se ignora", clave);
            return;
        }
        Reserva reserva = buscar(req.reservaId());
        if (!reserva.getEventoId().equals(eventoId) || reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new OperacionInvalidaException("La reserva " + req.reservaId() + " no corresponde a una venta del evento");
        }
        boolean estabaAgotado = sectorRepository.totalDisponibles(eventoId) == 0;
        if (reserva.getTipoSector() == TipoSector.NUMERADO) {
            if (req.asientoId() == null) {
                throw new IllegalArgumentException("Falta asientoId para un sector numerado");
            }
            asientoRepository.liberarVendido(eventoId, req.asientoId());
        }
        sectorRepository.reponer(eventoId, req.sectorId(), req.cantidad());
        reserva = buscar(req.reservaId());
        reserva.registrarDevolucion(req.cantidad());
        liberacionRepository.save(new Liberacion(clave, eventoId, req.reservaId(), req.sectorId(), req.asientoId(),
                req.cantidad()));
        if (estabaAgotado) {
            despuesDelCommit(() -> eventosClient.marcarDisponible(eventoId));
        }
        log.info("Liberado lugar vendido: evento={}, reserva={}, asiento={}", eventoId, req.reservaId(), req.asientoId());
    }

    private Reserva buscar(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva " + id));
    }

    private ReservaResponse armarRespuesta(Reserva r) {
        EventoDisponibilidad evento = eventoRepository.findById(r.getEventoId()).orElse(null);
        String sector = sectorRepository.findByEventoIdAndSectorId(r.getEventoId(), r.getSectorId())
                .map(SectorDisponibilidad::getNombre).orElse(null);
        return ReservaResponse.de(r, evento == null ? null : evento.getNombreEvento(),
                evento == null ? null : evento.getFechaEvento(), sector);
    }

    private void despuesDelCommit(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
    }
}
