package com.ticketya.recomendaciones.mensajeria;

import com.ticketya.recomendaciones.config.MensajeriaConfig;
import com.ticketya.recomendaciones.service.CatalogoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Comunicacion asincronica: el read model se actualiza con los eventos de dominio. */
@Component
public class EventosListener {

    private final CatalogoService catalogo;

    public EventosListener(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    @RabbitListener(queues = MensajeriaConfig.COLA_PUBLICADO)
    public void publicado(Mensajes.EventoPublicado m) {
        catalogo.eventoPublicado(m);
    }

    @RabbitListener(queues = MensajeriaConfig.COLA_CANCELADO)
    public void cancelado(Mensajes.EventoCancelado m) {
        catalogo.eventoCancelado(m);
    }

    @RabbitListener(queues = MensajeriaConfig.COLA_REPROGRAMADO)
    public void reprogramado(Mensajes.EventoReprogramado m) {
        catalogo.eventoReprogramado(m);
    }

    @RabbitListener(queues = MensajeriaConfig.COLA_COMPRA)
    public void compra(Mensajes.CompraConfirmada m) {
        catalogo.compraConfirmada(m);
    }
}
