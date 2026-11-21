package com.ticketya.bitacora.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Todos los servicios publican sus operaciones criticas en el exchange topic "ticketya.bitacora"
 * (routing key = nombre de la operacion, ej. "compra.aprobada"). Esta cola se suscribe a todo ("#").
 */
@Configuration
public class MensajeriaConfig {

    public static final String EXCHANGE = "ticketya.bitacora";
    public static final String COLA = "bitacora.registros";
    public static final String DLX = "ticketya.dlx";

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }

    @Bean
    public Declarables topologia() {
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        DirectExchange dlx = new DirectExchange(DLX, true, false);
        Queue cola = QueueBuilder.durable(COLA).deadLetterExchange(DLX).deadLetterRoutingKey(COLA + ".dlq").build();
        Queue dlq = QueueBuilder.durable(COLA + ".dlq").build();
        return new Declarables(exchange, dlx, cola, dlq,
                BindingBuilder.bind(cola).to(exchange).with("#"),
                BindingBuilder.bind(dlq).to(dlx).with(COLA + ".dlq"));
    }
}
