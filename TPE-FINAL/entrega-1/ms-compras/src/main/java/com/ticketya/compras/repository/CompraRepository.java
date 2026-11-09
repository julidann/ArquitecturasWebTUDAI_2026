package com.ticketya.compras.repository;

import com.ticketya.compras.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    Optional<Compra> findByIdempotencyKey(String idempotencyKey);

    Optional<Compra> findByReservaId(Long reservaId);

    List<Compra> findByUsuarioIdOrderByCreadaEnDesc(Long usuarioId);
}
