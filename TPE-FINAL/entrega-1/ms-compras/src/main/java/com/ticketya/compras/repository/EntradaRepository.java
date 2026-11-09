package com.ticketya.compras.repository;

import com.ticketya.compras.model.Entrada;
import com.ticketya.compras.model.EstadoEntrada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {

    List<Entrada> findByUsuarioIdOrderByEmitidaEnDesc(Long usuarioId);

    List<Entrada> findByEventoIdAndEstado(Long eventoId, EstadoEntrada estado);
}
