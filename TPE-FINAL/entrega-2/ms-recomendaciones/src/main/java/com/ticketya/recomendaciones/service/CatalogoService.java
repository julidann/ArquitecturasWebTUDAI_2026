package com.ticketya.recomendaciones.service;

import com.ticketya.recomendaciones.mensajeria.Mensajes;
import com.ticketya.recomendaciones.model.CompraHistorial;
import com.ticketya.recomendaciones.model.EventoCatalogo;
import com.ticketya.recomendaciones.repository.CompraHistorialRepository;
import com.ticketya.recomendaciones.repository.EventoCatalogoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Mantiene el read model (eventos + historial de compras) a partir de los mensajes. Todas las
 * operaciones son idempotentes porque usan el id del evento / de la compra como _id: si un
 * mensaje llega dos veces se vuelve a escribir el mismo documento.
 */
@Service
public class CatalogoService {

    private static final Logger log = LoggerFactory.getLogger(CatalogoService.class);

    private final EventoCatalogoRepository eventos;
    private final CompraHistorialRepository historial;
    private final Clock clock;

    public CatalogoService(EventoCatalogoRepository eventos, CompraHistorialRepository historial, Clock clock) {
        this.eventos = eventos;
        this.historial = historial;
        this.clock = clock;
    }

    public void eventoPublicado(Mensajes.EventoPublicado m) {
        eventos.save(new EventoCatalogo(m.eventoId(), m.nombre(), m.tipo(), m.participantes(), m.lugar(), m.ciudad(),
                m.fechaHora(), m.precioMinimo()));
        log.info("Evento {} agregado al catalogo de recomendaciones", m.eventoId());
    }

    public void eventoCancelado(Mensajes.EventoCancelado m) {
        eventos.findById(m.eventoId()).ifPresent(e -> {
            e.cancelar();
            eventos.save(e);
        });
    }

    public void eventoReprogramado(Mensajes.EventoReprogramado m) {
        eventos.findById(m.eventoId()).ifPresent(e -> {
            e.reprogramar(m.fechaNueva());
            eventos.save(e);
        });
    }

    public void compraConfirmada(Mensajes.CompraConfirmada m) {
        historial.save(new CompraHistorial(m.compraId(), m.usuarioId(), m.eventoId(), m.cantidad(),
                LocalDateTime.now(clock)));
    }
}
