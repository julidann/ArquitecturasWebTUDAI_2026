package com.ticketya.compras.repository;

import com.ticketya.compras.model.RegistroAcceso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroAccesoRepository extends JpaRepository<RegistroAcceso, Long> {

    long countByEventoIdAndResultado(Long eventoId, String resultado);

    long countByEventoId(Long eventoId);
}
