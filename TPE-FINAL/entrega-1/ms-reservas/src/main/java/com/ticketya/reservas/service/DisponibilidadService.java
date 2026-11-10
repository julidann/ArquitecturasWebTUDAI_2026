package com.ticketya.reservas.service;

import com.ticketya.reservas.dto.AsientoEstadoResponse;
import com.ticketya.reservas.dto.DisponibilidadInicialRequest;
import com.ticketya.reservas.dto.DisponibilidadResponse;
import com.ticketya.reservas.exception.OperacionInvalidaException;
import com.ticketya.reservas.exception.RecursoNoEncontradoException;
import com.ticketya.reservas.model.*;
import com.ticketya.reservas.repository.AsientoDisponibilidadRepository;
import com.ticketya.reservas.repository.EventoDisponibilidadRepository;
import com.ticketya.reservas.repository.ReservaRepository;
import com.ticketya.reservas.repository.SectorDisponibilidadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventario de lugares por evento: se crea al publicar el evento y se consulta para
 * mostrar el mapa de asientos. Esta informacion NO se cachea porque cambia todo el tiempo
 * durante una venta masiva (se justifica en docs/02-arquitectura.md).
 */
@Service
public class DisponibilidadService {

    private static final Logger log = LoggerFactory.getLogger(DisponibilidadService.class);

    private final EventoDisponibilidadRepository eventoRepository;
    private final SectorDisponibilidadRepository sectorRepository;
    private final AsientoDisponibilidadRepository asientoRepository;
    private final ReservaRepository reservaRepository;
    private final ReservaService reservaService;

    public DisponibilidadService(EventoDisponibilidadRepository eventoRepository,
                                 SectorDisponibilidadRepository sectorRepository,
                                 AsientoDisponibilidadRepository asientoRepository,
                                 ReservaRepository reservaRepository,
                                 ReservaService reservaService) {
        this.eventoRepository = eventoRepository;
        this.sectorRepository = sectorRepository;
        this.asientoRepository = asientoRepository;
        this.reservaRepository = reservaRepository;
        this.reservaService = reservaService;
    }

    /**
     * Crea el inventario. Es idempotente: si ms-eventos reintenta la publicacion y el
     * inventario ya existe, no se duplica nada.
     *
     * @return true si se creo, false si ya existia
     */
    @Transactional
    public boolean inicializar(DisponibilidadInicialRequest req) {
        if (eventoRepository.existsById(req.eventoId())) {
            log.info("La disponibilidad del evento {} ya estaba inicializada", req.eventoId());
            return false;
        }
        eventoRepository.save(new EventoDisponibilidad(req.eventoId(), req.nombreEvento(), req.fechaEvento(),
                req.limitePorCliente()));
        List<AsientoDisponibilidad> asientos = new ArrayList<>();
        for (DisponibilidadInicialRequest.SectorInicial s : req.sectores()) {
            if (s.tipo() == TipoSector.NUMERADO && (s.asientos() == null || s.asientos().isEmpty())) {
                throw new IllegalArgumentException("El sector numerado " + s.sectorId() + " no tiene asientos");
            }
            int capacidad = s.tipo() == TipoSector.NUMERADO ? s.asientos().size() : s.capacidad();
            sectorRepository.save(new SectorDisponibilidad(req.eventoId(), s.sectorId(), s.nombre(), s.tipo(),
                    s.precio(), capacidad));
            if (s.tipo() == TipoSector.NUMERADO) {
                s.asientos().forEach(a -> asientos.add(new AsientoDisponibilidad(req.eventoId(), s.sectorId(),
                        a.asientoId(), a.fila(), a.numero())));
            }
        }
        asientoRepository.saveAll(asientos);
        log.info("Disponibilidad inicializada para el evento {}: {} sectores, {} asientos numerados",
                req.eventoId(), req.sectores().size(), asientos.size());
        return true;
    }

    /**
     * Habilita o deshabilita la venta (suspension / cancelacion del evento). Al
     * deshabilitar se liberan las reservas pendientes para que nadie pueda pagarlas.
     */
    @Transactional
    public void cambiarHabilitacion(Long eventoId, boolean habilitado) {
        EventoDisponibilidad evento = buscarEvento(eventoId);
        evento.setHabilitado(habilitado);
        eventoRepository.saveAndFlush(evento);
        if (!habilitado) {
            List<Long> pendientes = reservaRepository.idsPendientesDeEvento(eventoId);
            pendientes.forEach(id -> reservaService.liberar(id, EstadoReserva.LIBERADA));
            log.info("Venta deshabilitada para el evento {}. Reservas pendientes liberadas: {}", eventoId,
                    pendientes.size());
        }
    }

    @Transactional(readOnly = true)
    public DisponibilidadResponse consultar(Long eventoId) {
        EventoDisponibilidad evento = buscarEvento(eventoId);
        List<DisponibilidadResponse.SectorDisponible> sectores = sectorRepository.findByEventoIdOrderBySectorId(eventoId)
                .stream()
                .map(s -> new DisponibilidadResponse.SectorDisponible(s.getSectorId(), s.getNombre(), s.getTipo(),
                        s.getPrecio(), s.getCapacidad(), s.getDisponibles()))
                .toList();
        return new DisponibilidadResponse(eventoId, evento.getNombreEvento(), evento.getFechaEvento(),
                evento.isHabilitado(), sectores);
    }

    @Transactional(readOnly = true)
    public List<AsientoEstadoResponse> asientos(Long eventoId, Long sectorId) {
        SectorDisponibilidad sector = sectorRepository.findByEventoIdAndSectorId(eventoId, sectorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El evento " + eventoId + " no tiene el sector " + sectorId));
        if (sector.getTipo() != TipoSector.NUMERADO) {
            throw new OperacionInvalidaException("El sector " + sector.getNombre() + " no es numerado");
        }
        return asientoRepository.findByEventoIdAndSectorIdOrderByFilaAscNumeroAsc(eventoId, sectorId).stream()
                .map(AsientoEstadoResponse::de)
                .toList();
    }

    private EventoDisponibilidad buscarEvento(Long eventoId) {
        return eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay disponibilidad para el evento " + eventoId + " (¿está publicado?)"));
    }
}
