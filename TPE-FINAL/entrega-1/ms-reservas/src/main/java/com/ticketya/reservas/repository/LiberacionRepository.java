package com.ticketya.reservas.repository;

import com.ticketya.reservas.model.Liberacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiberacionRepository extends JpaRepository<Liberacion, Long> {

    boolean existsByClave(String clave);
}
