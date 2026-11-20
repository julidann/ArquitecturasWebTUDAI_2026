package com.ticketya.eventos.service;

import com.ticketya.eventos.client.ComprasClient;
import com.ticketya.eventos.client.DisponibilidadInicialRequest;
import com.ticketya.eventos.client.DisponibilidadInicialRequest.AsientoInicial;
import com.ticketya.eventos.client.DisponibilidadInicialRequest.SectorInicial;
import com.ticketya.eventos.client.ReservasClient;
import com.ticketya.eventos.config.CacheConfig;
import com.ticketya.eventos.dto.EventoRequest;
import com.ticketya.eventos.dto.EventoResponse;
import com.ticketya.eventos.dto.PrecioSectorRequest;
import com.ticketya.eventos.dto.PreciosRequest;
import com.ticketya.eventos.exception.AccesoDenegadoException;
import com.ticketya.eventos.exception.OperacionInvalidaException;
import com.ticketya.eventos.exception.RecursoNoEncontradoException;
import com.ticketya.eventos.mensajeria.Mensajes;
import com.ticketya.eventos.mensajeria.Outbox;
import com.ticketya.eventos.model.*;
import com.ticketya.eventos.repository.EventoRepository;
import com.ticketya.eventos.repository.LugarRepository;
import com.ticketya.eventos.trazabilidad.Bitacora;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ciclo de vida de los eventos (funcionalidades 1, 3 y parte de 7 y 11).
 *
 * BORRADOR --publicar--> PUBLICADO --(sin lugares)--> AGOTADO
 *    PUBLICADO/AGOTADO --suspender--> SUSPENDIDO --reanudar--> PUBLICADO
 *    PUBLICADO/AGOTADO/SUSPENDIDO --reprogramar--> (mismo estado, otra fecha)
 *    cualquiera salvo FINALIZADO --cancelar--> CANCELADO
 *
 * Cache: el detalle del evento se cachea (cache "eventos") y se invalida en cada cambio; cualquier
 * cambio tambien vacia el cache de busquedas (ver CacheConfig para los TTL).
 *
 * Mensajes: publicar, cancelar y reprogramar dejan un mensaje en el outbox (misma transaccion) que
 * despues se publica en RabbitMQ para la fila virtual, las notificaciones y las recomendaciones.
 */
@Service
public class EventoService {

    private static final Logger log = LoggerFactory.getLogger(EventoService.class);

    private final EventoRepository eventoRepository;
    private final LugarRepository lugarRepository;
    private final ReservasClient reservasClient;
    private final ComprasClient comprasClient;
    private final Outbox outbox;
    private final Bitacora bitacora;

    public EventoService(EventoRepository eventoRepository, LugarRepository lugarRepository,
                         ReservasClient reservasClient, ComprasClient comprasClient, Outbox outbox, Bitacora bitacora) {
        this.eventoRepository = eventoRepository;
        this.lugarRepository = lugarRepository;
        this.reservasClient = reservasClient;
        this.comprasClient = comprasClient;
        this.outbox = outbox;
        this.bitacora = bitacora;
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
    @Cacheable(cacheNames = CacheConfig.EVENTOS, key = "#id")
    public EventoResponse obtener(Long id) {
        return EventoResponse.de(buscar(id));
    }

    @Transactional
    @Caching(evict = {@CacheEvict(cacheNames = CacheConfig.EVENTOS, key = "#id"),
            @CacheEvict(cacheNames = CacheConfig.BUSQUEDAS, allEntries = true)})
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
    @Caching(evict = {@CacheEvict(cacheNames = CacheConfig.EVENTOS, key = "#id"),
            @CacheEvict(cacheNames = CacheConfig.BUSQUEDAS, allEntries = true)})
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
    @Caching(evict = {@CacheEvict(cacheNames = CacheConfig.EVENTOS, key = "#id"),
            @CacheEvict(cacheNames = CacheConfig.BUSQUEDAS, allEntries = true)})
    public EventoResponse publicar(Long id, Long usuarioId, Rol rol) {
        Evento evento = buscar(id);
        exigirDuenio(evento, usuarioId, rol);
        exigirEstado(evento, EstadoEvento.BORRADOR, "Solo se pueden publicar eventos en borrador");
        if (!evento.tienePrecioParaTodosLosSectores()) {
            throw new OperacionInvalidaException("El evento no tiene precio definido para todos los sectores");
        }
        reservasClient.inicializarDisponibilidad(armarDisponibilidad(evento));
        evento.cambiarEstado(EstadoEvento.PUBLICADO);
        outbox.registrar(Mensajes.PUBLICADO, new Mensajes.EventoPublicado(evento.getId(), evento.getNombre(),
                evento.getTipo().name(), evento.getOrganizadorId(), List.copyOf(evento.getParticipantes()),
                evento.getLugar().getNombre(), evento.getLugar().getCiudad(), evento.getFechaHora(),
                evento.isAltaDemanda(), evento.getPrecioMinimo()));
        log.info("Evento {} publicado", id);
        return EventoResponse.de(evento);
    }

    @Transactional
    @Caching(evict = {@CacheEvict(cacheNames = CacheConfig.EVENTOS, key = "#id"),
            @CacheEvict(cacheNames = CacheConfig.BUSQUEDAS, allEntries = true)})
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
    @Caching(evict = {@CacheEvict(cacheNames = CacheConfig.EVENTOS, key = "#id"),
            @CacheEvict(cacheNames = CacheConfig.BUSQUEDAS, allEntries = true)})
    public EventoResponse reanudar(Long id, Rol rol) {
        exigirRol(rol, Rol.ADMIN);
        Evento evento = buscar(id);
        exigirEstado(evento, EstadoEvento.SUSPENDIDO, "Solo se pueden reanudar eventos suspendidos");
        reservasClient.cambiarHabilitacion(id, true, "Reanudado");
        evento.cambiarEstado(EstadoEvento.PUBLICADO);
        return EventoResponse.de(evento);
    }

    /**
     * Cambio de fecha de un evento ya publicado (funcionalidad 11: "cambio de un evento"). Primero se
     * actualiza ms-reservas (sincronico: decide si se puede seguir reservando) y despues se publica
     * evento.reprogramado: ms-compras actualiza la fecha de sus compras (plazo de devolucion) y
     * ms-notificaciones avisa a los compradores. Esas dos partes son eventualmente consistentes.
     */
    @Transactional
    @Caching(evict = {@CacheEvict(cacheNames = CacheConfig.EVENTOS, key = "#id"),
            @CacheEvict(cacheNames = CacheConfig.BUSQUEDAS, allEntries = true)})
    public EventoResponse reprogramar(Long id, LocalDateTime nuevaFecha, String motivo, Long usuarioId, Rol rol) {
        Evento evento = buscar(id);
        exigirDuenio(evento, usuarioId, rol);
        if (!EnumSet.of(EstadoEvento.PUBLICADO, EstadoEvento.AGOTADO, EstadoEvento.SUSPENDIDO).contains(evento.getEstado())) {
            throw new OperacionInvalidaException("Solo se pueden reprogramar eventos publicados, agotados o suspendidos"
                    + " (los borradores se modifican con PUT)");
        }
        LocalDateTime anterior = evento.getFechaHora();
        if (anterior.equals(nuevaFecha)) {
            return EventoResponse.de(evento);
        }
        reservasClient.reprogramar(id, nuevaFecha);
        evento.reprogramar(nuevaFecha);
        outbox.registrar(Mensajes.REPROGRAMADO, new Mensajes.EventoReprogramado(id, evento.getNombre(), anterior,
                nuevaFecha, motivo));
        bitacora.registrar("evento.reprogramado", "OK", usuarioId,
                Map.of("eventoId", id, "fechaAnterior", anterior.toString(), "fechaNueva", nuevaFecha.toString()));
        log.info("Evento {} reprogramado de {} a {}", id, anterior, nuevaFecha);
        return EventoResponse.de(evento);
    }

    /**
     * Cancela el evento: deshabilita las ventas, y ms-compras pasa todas las entradas
     * vendidas a reintegro. Si el evento ya estaba cancelado se vuelve a propagar
     * (los dos servicios son idempotentes), asi el admin puede reintentar si algo fallo.
     */
    @Transactional
    @Caching(evict = {@CacheEvict(cacheNames = CacheConfig.EVENTOS, key = "#id"),
            @CacheEvict(cacheNames = CacheConfig.BUSQUEDAS, allEntries = true)})
    public EventoResponse cancelar(Long id, String motivo, Rol rol) {
        exigirRol(rol, Rol.ADMIN);
        Evento evento = buscar(id);
        if (evento.getEstado() == EstadoEvento.FINALIZADO) {
            throw new OperacionInvalidaException("No se puede cancelar un evento finalizado");
        }
        boolean yaEstabaCancelado = evento.getEstado() == EstadoEvento.CANCELADO;
        boolean teniaVentas = evento.getEstado() != EstadoEvento.BORRADOR;
        evento.cambiarEstado(EstadoEvento.CANCELADO);
        eventoRepository.saveAndFlush(evento);
        if (teniaVentas) {
            reservasClient.cambiarHabilitacion(id, false, motivo);
            comprasClient.notificarCancelacion(id, motivo);
            if (!yaEstabaCancelado) {
                outbox.registrar(Mensajes.CANCELADO, new Mensajes.EventoCancelado(id, evento.getNombre(), motivo));
            }
        }
        Map<String, Object> detalle = new HashMap<>();
        detalle.put("eventoId", id);
        detalle.put("motivo", motivo);
        detalle.put("teniaVentas", teniaVentas);
        bitacora.registrar("evento.cancelado", "OK", null, detalle);
        log.info("Evento {} cancelado. Motivo: {}", id, motivo);
        return EventoResponse.de(evento);
    }

    /** Lo llama ms-reservas cuando se vende el ultimo lugar disponible. */
    @Transactional
    @Caching(evict = {@CacheEvict(cacheNames = CacheConfig.EVENTOS, key = "#id"),
            @CacheEvict(cacheNames = CacheConfig.BUSQUEDAS, allEntries = true)})
    public void marcarAgotado(Long id) {
        Evento evento = buscar(id);
        if (evento.getEstado() == EstadoEvento.PUBLICADO) {
            evento.cambiarEstado(EstadoEvento.AGOTADO);
            log.info("Evento {} agotado", id);
        }
    }

    /** Lo llama ms-reservas si se liberan lugares (devolucion) de un evento agotado. */
    @Transactional
    @Caching(evict = {@CacheEvict(cacheNames = CacheConfig.EVENTOS, key = "#id"),
            @CacheEvict(cacheNames = CacheConfig.BUSQUEDAS, allEntries = true)})
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
                evento.getLimiteEntradasPorCliente(), sectores, evento.isAltaDemanda());
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
