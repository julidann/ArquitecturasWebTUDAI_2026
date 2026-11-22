package com.ticketya.recomendaciones.repository;

import com.ticketya.recomendaciones.model.EventoCatalogo;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EventoCatalogoRepository extends MongoRepository<EventoCatalogo, Long> {
}
