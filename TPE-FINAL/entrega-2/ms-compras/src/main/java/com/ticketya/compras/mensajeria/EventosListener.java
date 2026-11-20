package com.ticketya.compras.mensajeria;

import com.ticketya.compras.config.MensajeriaConfig;
import com.ticketya.compras.repository.CompraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consistencia eventual: cuando un evento se reprograma, ms-compras actualiza la fecha que tiene
 * copiada en sus compras (se usa para el plazo de 72 hs de las devoluciones). Es idempotente: aplicar
 * dos veces el mismo mensaje deja la misma fecha.
 */
@Component
public class EventosListener {

    private static final Logger log = LoggerFactory.getLogger(EventosListener.class);
    private final CompraRepository compraRepository;

    public EventosListener(CompraRepository compraRepository) {
        this.compraRepository = compraRepository;
    }

    @RabbitListener(queues = MensajeriaConfig.COLA_REPROGRAMADO)
    @Transactional
    public void eventoReprogramado(Mensajes.EventoReprogramado m) {
        int n = compraRepository.actualizarFechaEvento(m.eventoId(), m.fechaNueva());
        log.info("Evento {} reprogramado a {}: {} compras actualizadas", m.eventoId(), m.fechaNueva(), n);
    }
}
