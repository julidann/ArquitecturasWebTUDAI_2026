package com.ticketya.eventos.repository;

import com.ticketya.eventos.model.Sector;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, Long> {
}
