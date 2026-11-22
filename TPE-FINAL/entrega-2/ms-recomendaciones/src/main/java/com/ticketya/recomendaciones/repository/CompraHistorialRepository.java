package com.ticketya.recomendaciones.repository;

import com.ticketya.recomendaciones.model.CompraHistorial;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CompraHistorialRepository extends MongoRepository<CompraHistorial, Long> {

    List<CompraHistorial> findByUsuarioId(Long usuarioId);
}
