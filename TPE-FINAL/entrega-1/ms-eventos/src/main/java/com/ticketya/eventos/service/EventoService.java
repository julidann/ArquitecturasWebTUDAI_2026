package com.ticketya.eventos.service;

import com.ticketya.eventos.client.ComprasClient;
import com.ticketya.eventos.client.DisponibilidadInicialRequest;
import com.ticketya.eventos.client.DisponibilidadInicialRequest.AsientoInicial;
import com.ticketya.eventos.client.DisponibilidadInicialRequest.SectorInicial;
import com.ticketya.eventos.client.ReservasClient;
import com.ticketya.eventos.dto.EventoRequest;
import com.ticketya.eventos.dto.EventoResponse;
import com.ticketya.eventos.dto.PrecioSectorRequest;
import com.ticketya.eventos.dto.PreciosRequest;
import com.ticketya.eventos.exception.AccesoDenegadoException;
import com.ticketya.eventos.exception.OperacionInvalidaException;
import com.ticketya.eventos.exception.RecursoNoEncontradoException;
import com.ticketya.eventos.model.*;
import com.ticketya.eventos.repository.EventoRepository;
import com.ticketya.eventos.repository.LugarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

/**
 * Ciclo de vida de los eventos (funcionalidades 1, 3 y parte de 7).
 *
 * BORRADOR --publicar--> PUBLICADO --(sin lugares)--> AGOTADO
 *    PUBLICADO/AGOTADO --suspender--> SUSPENDIDO --reanudar--> PUBLICADO
 *    cualquiera salvo FINALIZADO --cancelar--> CANCELADO
 */
@Service
public class EventoService {

    private static final Logger log = LoggerFactory.getLogger(EventoService.class);

    private final EventoRepository eventoRepository;
    private final LugarRepository lugarRepository;
    private final ReservasClient reservasClient;
    private final ComprasClient comprasClient;

    public EventoService(EventoRepository eventoRepository, LugarRepository lugarRepository,
                         ReservasClient reservasClient, ComprasClient comprasClient) {
        this.eventoRepository = eventoRepository;
        this.lugarRepository = lugarRepository;
        this.reservasClient = reservasClient;
        this.comprasClient = comprasClient;
    }

    @Transactional
    public EventoResponse crear(EventoRequest request, Long usuarioId, Rol rol) {
        exigirRol(rol, Rol.ORGANIZADOR, Rol.ADMIN);
        Lugar lugar = lugarRepository.findById(request.lugarId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el lugar con id " + request.lugarId()));
        Evento evento = new Evento(request.nombre(), request.descripcion(), request.tipo(), usuarioId,
                request.participantes(), request.fechaHora(), lugar);
        evento.setAltaDemanda(Boolean.TRUE.equals(request.altaDemanda()));
        if (request.limiteEntradasPorCliente() != null) {
            evento.setLimiteEntradasPorCliente(request.limiteEntradasPorCliente());
        }
        evento = eventoRepository.save(evento);
        log.info("Evento {} creado en borrador por el organizador {}", evento.getId(), usuarioId);
        return EventoResponse.de(evento);
    }

    @Transactional(readOnly = true)
    public EventoResponse obtener(Long id) {
        return EventoResponse.de(buscar(id));
    }

    @Transactional
    public EventoResponse actualizar(Long id, EventoRequest request, Long usuarioId, Rol rol) {
        Evento evento = buscar(id);
        exigirDuenio(evento, usuarioId, rol);
        exigirEstado(evento, EstadoEvento.BORRADOR, "Solo se pueden modificar eventos en borrador");
        if (!evento.getLugar().getId().equals(request.lugarId())) {
            throw new OperacionInvalidaException("No se puede cambiar el lugar de un evento ya creado");
        }
        evento.actualizarDatos(request.nombre(), request.descripcion(), request.tipo(), request.participantes(),
                request.fechaHora(), Boolean.TRUE.equals(request.altaDemanda()), request.limiteEntradasPorCliente());
        return EventoResponse.de(evento);
    }

    @Transactional
    public EventoResponse definirPrecios(Long id, PreciosRequest request, Long usuarioId, Rol rol) {
        Evento evento = buscar(id);
        exigirDuenio(evento, usuarioId, rol);
        exigirEstado(evento, EstadoEvento.BORRADOR, "Los precios solo se pueden definir con el evento en borrador");
        for (PrecioSectorRequest p : request.precios()) {
            Sector sector = evento.getLugar().getSectores().stream()
                    .filter(s -> s.getId().equals(p.sectorId()))
                    .findFirst()
                    .orElseThrow(() -> new OperacionInvalidaException(
                            "El sector " + p.sectorId() + " no pertenece al lugar del evento"));
            evento.definirPrecio(sector, p.precio());
        }
        return EventoResponse.de(evento);
    }

    /**
     * Publica el evento. Antes de cambiar el estado se le pide a ms-reservas que arme
     * el inventario; si eso falla la transaccion hace rollback y el evento sigue en borrador.
     * Del lado de ms-reservas la inicializacion es idempotente, asi que reintentar es seguro.
     */
    @Transactional
    public EventoResponse publicar(Long id, Long usuarioId, Rol rol) {
        Evento evento = buscar(id);
        exigirDuenio(evento, usuarioId, rol);
        exigirEstado(evento, EstadoEvento.BORRADOR, "Solo se pueden publicar eventos en borrador");
        if (!evento.tienePrecioParaTodosLosSectores()) {
            throw new OperacionInvalidaException("El evento no tiene precio definido para todos los sectores");
        }
        reservasClient.inicializarDisponibilidad(armarDisponibilidad(evento));
        evento.cambiarEstado(EstadoEvento.PUBLICADO);
        log.info("Evento {} publicado", id);
        return EventoResponse.de(evento);
    }

    @Transactional
    public EventoResponse suspender(Long id, String motivo, Rol rol) {
        exigirRol(rol, Rol.ADMIN);
        Evento evento = buscar(id);
        if (!EnumSet.of(EstadoEvento.PUBLICADO, EstadoEvento.AGOTADO).contains(evento.getEstado())) {
            throw new OperacionInvalidaException("Solo se pueden suspender eventos publicados o agotados");
        }
        reservasClient.cambiarHabilitacion(id, false, motivo);
        evento.cambiarEstado(EstadoEvento.SUSPENDIDO);
        log.info("Evento {} suspendido. Motivo: {}", id, motivo);
        return EventoResponse.de(evento);
    }

    @Transactional
    public EventoResponse reanudar(Long id, Rol rol) {
        exigirRol(rol, Rol.ADMIN);
        Evento evento = buscar(id);
        exigirEstado(evento, EstadoEvento.SUSPENDIDO, "Solo se pueden reanudar eventos suspendidos");
        reservasClient.cambiarHabilitacion(id, true, "Reanudado");
        evento.cambiarEstado(EstadoEvento.PUBLICADO);
        return EventoResponse.de(evento);
    }

    /**
     * Cancela el evento: deshabilita las ventas, y ms-compras pasa todas las entradas
     * vendidas a reintegro. Si el evento ya estaba cancelado se vuelve a propagar
     * (los dos servicios son idempotentes), asi el admin puede reintentar si algo fallo.
     */
    @Transactional
    public EventoResponse cancelar(Long id, String motivo, Rol rol) {
        exigirRol(rol, Rol.ADMIN);
        Evento evento = buscar(id);
        if (evento.getEstado() == EstadoEvento.FINALIZADO) {
            throw new OperacionInvalidaException("No se puede cancelar un evento finalizado");
        }
        boolean teniaVentas = evento.getEstado() != EstadoEvento.BORRADOR;
        evento.cambiarEstado(EstadoEvento.CANCELADO);
        eventoRepository.saveAndFlush(evento);
        if (teniaVentas) {
            reservasClient.cambiarHabilitacion(id, false, motivo);
            comprasClient.notificarCancelacion(id, motivo);
        }
        log.info("Evento {} cancelado. Motivo: {}", id, motivo);
        return EventoResponse.de(evento);
    }

    /** Lo llama ms-reservas cuando se vende el ultimo lugar disponible. */
    @Transactional
    public void marcarAgotado(Long id) {
        Evento evento = buscar(id);
        if (evento.getEstado() == EstadoEvento.PUBLICADO) {
            evento.cambiarEstado(EstadoEvento.AGOTADO);
            log.info("Evento {} agotado", id);
        }
    }

    /** Lo llama ms-reservas si se liberan lugares (devolucion) de un evento agotado. */
    @Transactional
    public void marcarDisponible(Long id) {
        Evento evento = buscar(id);
        if (evento.getEstado() == EstadoEvento.AGOTADO) {
            evento.cambiarEstado(EstadoEvento.PUBLICADO);
        }
    }

    private DisponibilidadInicialRequest armarDisponibilidad(Evento evento) {
        List<SectorInicial> sectores = evento.getPrecios().stream()
                .map(p -> {
                    Sector s = p.getSector();
                    List<AsientoInicial> asientos = s.getAsientos().stream()
                            .map(a -> new AsientoInicial(a.getId(), a.getFila(), a.getNumero()))
                            .toList();
                    return new SectorInicial(s.getId(), s.getNombre(), s.getTipo(), p.getPrecio(),
                            s.getCapacidad(), asientos);
                })
                .toList();
        return new DisponibilidadInicialRequest(evento.getId(), evento.getNombre(), evento.getFechaHora(),
                evento.getLimiteEntradasPorCliente(), sectores);
    }

    private Evento buscar(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el evento con id " + id));
    }

    private void exigirEstado(Evento evento, EstadoEvento esperado, String mensaje) {
        if (evento.getEstado() != esperado) {
            throw new OperacionInvalidaException(mensaje + " (estado actual: " + evento.getEstado() + ")");
        }
    }

    private void exigirRol(Rol rol, Rol... permitidos) {
        if (!EnumSet.copyOf(List.of(permitidos)).contains(rol)) {
            throw new AccesoDenegadoException("Operacion no permitida para el rol " + rol);
        }
    }

    /** El organizador solo puede tocar sus propios eventos; el admin puede tocar cualquiera. */
    private void exigirDuenio(Evento evento, Long usuarioId, Rol rol) {
        if (rol == Rol.ADMIN) {
            return;
        }
        if (rol != Rol.ORGANIZADOR || !evento.getOrganizadorId().equals(usuarioId)) {
            throw new AccesoDenegadoException("Solo el organizador del evento puede realizar esta operacion");
        }
    }
}
