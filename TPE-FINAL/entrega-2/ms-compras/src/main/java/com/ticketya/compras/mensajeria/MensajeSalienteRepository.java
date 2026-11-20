package com.ticketya.compras.mensajeria;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeSalienteRepository extends JpaRepository<MensajeSaliente, Long> {

    List<MensajeSaliente> findByPublicadoEnIsNullOrderByIdAsc(Pageable pageable);

    long countByPublicadoEnIsNull();
}
