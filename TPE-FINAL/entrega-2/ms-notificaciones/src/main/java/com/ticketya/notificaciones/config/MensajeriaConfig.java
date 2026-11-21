package com.ticketya.notificaciones.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketya.notificaciones.trazabilidad.CorrelacionFilter;
import org.slf4j.MDC;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Mensajeria (RabbitMQ). Este servicio consume eventos de dominio del exchange "ticketya.eventos".
 * Cada tipo de mensaje tiene su propia cola (asi el tipo del parametro del listener define como se
 * deserializa el JSON). Los mensajes que fallan 3 veces van a una cola ".dlq" para revisarlos.
 */
@Configuration
public class MensajeriaConfig {

    public static final String EXCHANGE_EVENTOS = "ticketya.eventos";
    public static final String DLX = "ticketya.dlx";

    public static final String COLA_PUBLICADO = "notificaciones.evento-publicado";
    public static final String COLA_CANCELADO = "notificaciones.evento-cancelado";
    public static final String COLA_REPROGRAMADO = "notificaciones.evento-reprogramado";
    public static final String COLA_COMPRA = "notificaciones.compra-confirmada";

    /** {cola, routing key} */
    private static final String[][] COLAS = {
            {COLA_PUBLICADO, "evento.publicado"},
            {COLA_CANCELADO, "evento.cancelado"},
            {COLA_REPROGRAMADO, "evento.reprogramado"},
            {COLA_COMPRA, "compra.confirmada"}};

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        // el tipo lo decide el parametro del @RabbitListener, no un header con el nombre de clase del emisor
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }

    /** Recupera el X-Correlation-Id del mensaje para que los logs del consumidor lo incluyan. */
    @Bean
    public ContainerCustomizer<SimpleMessageListenerContainer> correlacionEnMensajes() {
        return container -> container.setAfterReceivePostProcessors(mensaje -> {
            Object id = mensaje.getMessageProperties().getHeader(CorrelacionFilter.HEADER);
            if (id != null) {
                MDC.put(CorrelacionFilter.MDC_CLAVE, id.toString());
            } else {
                MDC.remove(CorrelacionFilter.MDC_CLAVE);
            }
            return mensaje;
        });
    }

    @Bean
    public Declarables topologia() {
        TopicExchange eventos = new TopicExchange(EXCHANGE_EVENTOS, true, false);
        DirectExchange dlx = new DirectExchange(DLX, true, false);
        List<Declarable> d = new ArrayList<>(List.of(eventos, dlx));
        for (String[] cola : COLAS) {
            Queue q = QueueBuilder.durable(cola[0]).deadLetterExchange(DLX).deadLetterRoutingKey(cola[0] + ".dlq").build();
            Queue dlq = QueueBuilder.durable(cola[0] + ".dlq").build();
            d.add(q);
            d.add(dlq);
            d.add(BindingBuilder.bind(q).to(eventos).with(cola[1]));
            d.add(BindingBuilder.bind(dlq).to(dlx).with(cola[0] + ".dlq"));
        }
        return new Declarables(d);
    }
}

