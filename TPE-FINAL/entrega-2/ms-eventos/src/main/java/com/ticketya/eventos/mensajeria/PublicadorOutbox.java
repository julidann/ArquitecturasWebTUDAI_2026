package com.ticketya.eventos.mensajeria;

import com.ticketya.eventos.config.MensajeriaConfig;
import com.ticketya.eventos.trazabilidad.CorrelacionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Publica en RabbitMQ los mensajes pendientes del outbox (cada segundo). Si RabbitMQ no responde se
 * corta y se reintenta en la proxima vuelta. La entrega es "al menos una vez": si el servicio se cae
 * justo despues de publicar y antes de marcar, el mensaje sale de nuevo; por eso todos los
 * consumidores son idempotentes.
 */
@Component
@ConditionalOnProperty(name = "outbox.publicar", havingValue = "true", matchIfMissing = true)
public class PublicadorOutbox {

    private static final Logger log = LoggerFactory.getLogger(PublicadorOutbox.class);

    private final MensajeSalienteRepository repository;
    private final RabbitTemplate rabbit;
    private final TransactionTemplate tx;

    public PublicadorOutbox(MensajeSalienteRepository repository, RabbitTemplate rabbit, TransactionTemplate tx) {
        this.repository = repository;
        this.rabbit = rabbit;
        this.tx = tx;
    }

    @Scheduled(fixedDelayString = "${outbox.intervalo-ms:1000}")
    public void publicarPendientes() {
        List<MensajeSaliente> pendientes = repository.findByPublicadoEnIsNullOrderByIdAsc(PageRequest.of(0, 100));
        for (MensajeSaliente m : pendientes) {
            try {
                MessageProperties props = new MessageProperties();
                props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                props.setContentEncoding("UTF-8");
                props.setMessageId("outbox-" + m.getId());
                if (m.getCorrelationId() != null) {
                    props.setHeader(CorrelacionFilter.HEADER, m.getCorrelationId());
                }
                rabbit.send(MensajeriaConfig.EXCHANGE_EVENTOS, m.getRoutingKey(),
                        new Message(m.getPayload().getBytes(StandardCharsets.UTF_8), props));
                tx.executeWithoutResult(s -> repository.findById(m.getId()).ifPresent(MensajeSaliente::marcarPublicado));
            } catch (RuntimeException e) {
                tx.executeWithoutResult(s -> repository.findById(m.getId()).ifPresent(MensajeSaliente::registrarIntento));
                log.warn("No se pudo publicar el mensaje {} ({}): {}. Se reintenta luego.", m.getId(), m.getRoutingKey(),
                        e.getMessage());
                return;
            }
        }
    }
}
