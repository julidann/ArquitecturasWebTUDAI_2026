package com.ticketya.pagos.repository;

import com.ticketya.pagos.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByIdempotencyKey(String idempotencyKey);

    List<Pago> findByCompraIdOrderByIdDesc(Long compraId);
}
