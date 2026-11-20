package com.ticketya.eventos.mensajeria;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketya.eventos.trazabilidad.CorrelacionFilter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Registra un mensaje de dominio para publicar (tiene que llamarse dentro de la transaccion del cambio). */
@Component
public class Outbox {

    private final MensajeSalienteRepository repository;
    private final ObjectMapper mapper;

    public Outbox(MensajeSalienteRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String routingKey, Object mensaje) {
        try {
            repository.save(new MensajeSaliente(routingKey, mapper.writeValueAsString(mensaje), CorrelacionFilter.actual()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el mensaje " + routingKey, e);
        }
    }
}
