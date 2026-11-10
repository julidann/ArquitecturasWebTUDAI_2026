package com.ticketya.reservas.repository;

import com.ticketya.reservas.model.EventoDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoDisponibilidadRepository extends JpaRepository<EventoDisponibilidad, Long> {
}
