package com.ticketya.fila.mensajeria;

import com.ticketya.fila.config.MensajeriaConfig;
import com.ticketya.fila.service.FilaService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Comunicacion asincronica: la fila se abre sola cuando ms-eventos publica un evento de alta demanda
 * y se cierra si el evento se cancela. Las dos operaciones son idempotentes (si el mensaje llega
 * dos veces no pasa nada).
 */
@Component
public class EventosListener {

    private final FilaService filaService;

    public EventosListener(FilaService filaService) {
        this.filaService = filaService;
    }

    @RabbitListener(queues = MensajeriaConfig.COLA_PUBLICADO)
    public void eventoPublicado(EventoPublicadoMensaje m) {
        if (m.altaDemanda()) {
            filaService.abrir(m.eventoId(), m.organizadorId());
        }
    }

    @RabbitListener(queues = MensajeriaConfig.COLA_CANCELADO)
    public void eventoCancelado(EventoCanceladoMensaje m) {
        filaService.cerrar(m.eventoId());
    }
}
