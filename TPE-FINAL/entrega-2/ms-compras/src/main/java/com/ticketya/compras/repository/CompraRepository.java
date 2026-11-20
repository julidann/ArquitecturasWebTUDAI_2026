package com.ticketya.compras.repository;

import com.ticketya.compras.model.Compra;
import com.ticketya.compras.model.EstadoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    Optional<Compra> findByIdempotencyKey(String idempotencyKey);

    Optional<Compra> findByReservaId(Long reservaId);

    List<Compra> findByUsuarioIdOrderByCreadaEnDesc(Long usuarioId);

    /** Compras que quedaron a mitad de camino (ERROR) hace un rato: las retoma el job de recuperacion. */
    @Query("select c.id from Compra c where c.estado = com.ticketya.compras.model.EstadoCompra.ERROR "
            + "and c.actualizadaEn < :antesDe order by c.id")
    List<Long> idsEnErrorAntesDe(@Param("antesDe") LocalDateTime antesDe);

    /**
     * "Reclama" una compra en ERROR para reprocesarla: solo un proceso (un reintento del cliente o el
     * job de alguna de las instancias) logra pasarla a PENDIENTE; los demas ven 0 filas y no la tocan.
     */
    @Modifying
    @Query("update Compra c set c.estado = com.ticketya.compras.model.EstadoCompra.PENDIENTE, c.actualizadaEn = :ahora "
            + "where c.id = :id and c.estado = com.ticketya.compras.model.EstadoCompra.ERROR")
    int reclamarParaReintento(@Param("id") Long id, @Param("ahora") LocalDateTime ahora);

    @Modifying
    @Query("update Compra c set c.fechaEvento = :fecha where c.eventoId = :eventoId")
    int actualizarFechaEvento(@Param("eventoId") Long eventoId, @Param("fecha") LocalDateTime fecha);

    long countByEstado(EstadoCompra estado);
}
