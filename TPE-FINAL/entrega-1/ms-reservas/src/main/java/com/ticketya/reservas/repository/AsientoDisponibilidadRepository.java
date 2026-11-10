package com.ticketya.reservas.repository;

import com.ticketya.reservas.model.AsientoDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AsientoDisponibilidadRepository extends JpaRepository<AsientoDisponibilidad, Long> {

    List<AsientoDisponibilidad> findByEventoIdAndSectorIdOrderByFilaAscNumeroAsc(Long eventoId, Long sectorId);

    List<AsientoDisponibilidad> findByEventoIdAndAsientoIdIn(Long eventoId, Collection<Long> asientoIds);

    boolean existsByEventoId(Long eventoId);

    /**
     * Mecanismo principal contra la doble venta de asientos numerados.
     *
     * Es un UPDATE condicional (compare-and-set): solo pasa a RESERVADO los asientos que
     * siguen DISPONIBLES. La base bloquea cada fila mientras la actualiza, entonces si dos
     * transacciones (de la misma o de distintas instancias) quieren el mismo asiento, la
     * segunda espera, vuelve a evaluar el WHERE y ya no lo encuentra disponible.
     * El service compara la cantidad de filas afectadas con la cantidad pedida y si no
     * coincide hace rollback de toda la reserva (todo o nada).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AsientoDisponibilidad a
               set a.estado = com.ticketya.reservas.model.EstadoAsiento.RESERVADO,
                   a.reservaId = :reservaId, a.version = a.version + 1
             where a.eventoId = :eventoId and a.sectorId = :sectorId and a.asientoId in :asientoIds
               and a.estado = com.ticketya.reservas.model.EstadoAsiento.DISPONIBLE
            """)
    int reservar(@Param("eventoId") Long eventoId, @Param("sectorId") Long sectorId,
                 @Param("asientoIds") Collection<Long> asientoIds, @Param("reservaId") Long reservaId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AsientoDisponibilidad a
               set a.estado = com.ticketya.reservas.model.EstadoAsiento.VENDIDO, a.version = a.version + 1
             where a.reservaId = :reservaId and a.estado = com.ticketya.reservas.model.EstadoAsiento.RESERVADO
            """)
    int venderDeReserva(@Param("reservaId") Long reservaId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AsientoDisponibilidad a
               set a.estado = com.ticketya.reservas.model.EstadoAsiento.DISPONIBLE, a.reservaId = null,
                   a.version = a.version + 1
             where a.reservaId = :reservaId and a.estado = com.ticketya.reservas.model.EstadoAsiento.RESERVADO
            """)
    int liberarDeReserva(@Param("reservaId") Long reservaId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AsientoDisponibilidad a
               set a.estado = com.ticketya.reservas.model.EstadoAsiento.DISPONIBLE, a.reservaId = null,
                   a.version = a.version + 1
             where a.eventoId = :eventoId and a.asientoId = :asientoId
               and a.estado = com.ticketya.reservas.model.EstadoAsiento.VENDIDO
            """)
    int liberarVendido(@Param("eventoId") Long eventoId, @Param("asientoId") Long asientoId);
}
