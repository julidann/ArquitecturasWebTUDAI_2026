package com.ticketya.pagos.repository;

import com.ticketya.pagos.model.Reintegro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReintegroRepository extends JpaRepository<Reintegro, Long> {

    Optional<Reintegro> findByIdempotencyKey(String idempotencyKey);
}
