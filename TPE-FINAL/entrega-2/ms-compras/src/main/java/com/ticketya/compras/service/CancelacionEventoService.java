package com.ticketya.compras.service;

import com.ticketya.compras.client.PagosClient;
import com.ticketya.compras.dto.CancelacionEventoResponse;
import com.ticketya.compras.model.*;
import com.ticketya.compras.repository.EntradaRepository;
import com.ticketya.compras.repository.SolicitudDevolucionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cancelacion de un evento (funcionalidad 7): todas las entradas validas pasan a
 * CANCELADA y se pide el reintegro de cada una. Si algun reintegro falla, esa entrada
 * queda VALIDA y se reprocesa cuando ms-eventos vuelva a llamar (el admin puede
 * reintentar la cancelacion); las claves de idempotencia evitan reintegrar dos veces.
 *
 * La notificacion a los compradores la hace ms-notificaciones al recibir el mensaje evento.cancelado
 * (asincronico): consulta a este servicio la lista de compradores y les manda el aviso.
 */
@Service
public class CancelacionEventoService {

    private static final Logger log = LoggerFactory.getLogger(CancelacionEventoService.class);

    private final EntradaRepository entradaRepository;
    private final SolicitudDevolucionRepository solicitudRepository;
    private final PagosClient pagosClient;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final com.ticketya.compras.trazabilidad.Bitacora bitacora;

    public CancelacionEventoService(EntradaRepository entradaRepository,
                                    SolicitudDevolucionRepository solicitudRepository, PagosClient pagosClient,
                                    TransactionTemplate tx, Clock clock,
                                    com.ticketya.compras.trazabilidad.Bitacora bitacora) {
        this.bitacora = bitacora;
        this.entradaRepository = entradaRepository;
        this.solicitudRepository = solicitudRepository;
        this.pagosClient = pagosClient;
        this.tx = tx;
        this.clock = clock;
    }

    public CancelacionEventoResponse cancelarEvento(Long eventoId, String motivo) {
        List<Entrada> entradas = tx.execute(s -> {
            List<Entrada> lista = entradaRepository.findByEventoIdAndEstado(eventoId, EstadoEntrada.VALIDA);
            lista.forEach(e -> e.getCompra().getPagoId());
            return lista;
        });
        int ok = 0;
        int fallidos = 0;
        Set<Long> compradores = new HashSet<>();
        for (Entrada e : entradas) {
            try {
                pagosClient.reintegrar(e.getCompra().getPagoId(), e.getPrecio(),
                        "Cancelación del evento " + eventoId, "cancelacion-entrada-" + e.getId());
                tx.executeWithoutResult(s -> entradaRepository.findById(e.getId()).orElseThrow().cancelarPorEvento());
                compradores.add(e.getUsuarioId());
                ok++;
            } catch (RuntimeException ex) {
                fallidos++;
                log.error("No se pudo reintegrar la entrada {} del evento cancelado {}: {}", e.getId(), eventoId,
                        ex.getMessage());
            }
        }
        // las solicitudes de devolucion pendientes ya no tienen sentido: el reintegro es automatico
        tx.executeWithoutResult(s -> solicitudRepository.findByEntradaEventoIdAndEstado(eventoId, EstadoSolicitud.PENDIENTE)
                .forEach(sol -> sol.rechazar(null, "Evento cancelado: el reintegro se hizo automáticamente",
                        LocalDateTime.now(clock))));
        log.info("Evento {} cancelado: {} entradas canceladas, {} reintegros fallidos", eventoId, ok, fallidos);
        bitacora.registrar("cancelacion.reintegros", fallidos == 0 ? "OK" : "PARCIAL", null,
                java.util.Map.of("eventoId", eventoId, "entradasCanceladas", ok, "reintegrosFallidos", fallidos,
                        "compradores", compradores.size()));
        return new CancelacionEventoResponse(eventoId, ok, ok, fallidos, compradores.size());
    }
}
