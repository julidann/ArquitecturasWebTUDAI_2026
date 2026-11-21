package com.ticketya.bitacora.repository;

import com.ticketya.bitacora.model.Registro;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RegistroRepository extends MongoRepository<Registro, String> {

    List<Registro> findByCorrelationIdOrderByFechaAsc(String correlationId);
}
