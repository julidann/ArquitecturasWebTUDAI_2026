package com.ticketya.reservas.repository;

import com.ticketya.reservas.model.SectorDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SectorDisponibilidadRepository extends JpaRepository<SectorDisponibilidad, Long> {

    Optional<SectorDisponibilidad> findByEventoIdAndSectorId(Long eventoId, Long sectorId);

    List<SectorDisponibilidad> findByEventoIdOrderBySectorId(Long eventoId);

    /**
     * Descuenta cupo en forma atomica. El WHERE "disponibles >= :cantidad" se evalua en la
     * base con la fila bloqueada, por lo que dos instancias del servicio no pueden dejar el
     * cupo en negativo: si no alcanza, el UPDATE afecta 0 filas.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update SectorDisponibilidad s
               set s.disponibles = s.disponibles - :cantidad, s.version = s.version + 1
             where s.eventoId = :eventoId and s.sectorId = :sectorId and s.disponibles >= :cantidad
            """)
    int descontar(@Param("eventoId") Long eventoId, @Param("sectorId") Long sectorId,
                  @Param("cantidad") int cantidad);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update SectorDisponibilidad s
               set s.disponibles = s.disponibles + :cantidad, s.version = s.version + 1
             where s.eventoId = :eventoId and s.sectorId = :sectorId
               and s.disponibles + :cantidad <= s.capacidad
            """)
    int reponer(@Param("eventoId") Long eventoId, @Param("sectorId") Long sectorId,
                @Param("cantidad") int cantidad);

    @Query("select coalesce(sum(s.disponibles), 0) from SectorDisponibilidad s where s.eventoId = :eventoId")
    long totalDisponibles(@Param("eventoId") Long eventoId);
}
