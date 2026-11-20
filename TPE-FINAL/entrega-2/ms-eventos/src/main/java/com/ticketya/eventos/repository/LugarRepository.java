package com.ticketya.eventos.repository;

import com.ticketya.eventos.model.Lugar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LugarRepository extends JpaRepository<Lugar, Long> {

    List<Lugar> findByCiudadIgnoreCase(String ciudad);
}
