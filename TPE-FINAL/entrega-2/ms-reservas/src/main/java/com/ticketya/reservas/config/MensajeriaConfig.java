package com.ticketya.reservas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketya.reservas.trazabilidad.Bitacora;
import com.ticketya.reservas.trazabilidad.CorrelacionFilter;
import org.slf4j.MDC;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RabbitMQ: mensajes en JSON y el X-Correlation-Id viaja como header de cada mensaje. */
@Configuration
public class MensajeriaConfig {

    public static final String EXCHANGE_EVENTOS = "ticketya.eventos";

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public TopicExchange exchangeEventos() {
        return new TopicExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    public TopicExchange exchangeBitacora() {
        return new TopicExchange(Bitacora.EXCHANGE, true, false);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setBeforePublishPostProcessors(mensaje -> {
            String id = MDC.get(CorrelacionFilter.MDC_CLAVE);
            if (id != null) {
                mensaje.getMessageProperties().setHeader(CorrelacionFilter.HEADER, id);
            }
            return mensaje;
        });
        return template;
    }
}
