package com.ticketya.reservas.repository;

import com.ticketya.reservas.model.EstadoReserva;
import com.ticketya.reservas.model.Reserva;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByUsuarioIdOrderByCreadaEnDesc(Long usuarioId);

    List<Reserva> findByUsuarioIdAndEstadoOrderByCreadaEnDesc(Long usuarioId, EstadoReserva estado);

    @Query("select r.id from Reserva r where r.eventoId = :eventoId and r.estado = com.ticketya.reservas.model.EstadoReserva.PENDIENTE")
    List<Long> idsPendientesDeEvento(@Param("eventoId") Long eventoId);

    @Query("""
            select r.id from Reserva r
             where r.estado = com.ticketya.reservas.model.EstadoReserva.PENDIENTE and r.expiraEn <= :ahora
             order by r.expiraEn
            """)
    List<Long> idsVencidas(@Param("ahora") LocalDateTime ahora, Pageable pageable);

    /** Entradas que el cliente tiene tomadas (reservas en curso + compradas no devueltas). */
    @Query("""
            select coalesce(sum(r.cantidad - r.cantidadDevuelta), 0) from Reserva r
             where r.usuarioId = :usuarioId and r.eventoId = :eventoId
               and r.estado in (com.ticketya.reservas.model.EstadoReserva.PENDIENTE,
                                com.ticketya.reservas.model.EstadoReserva.CONFIRMADA)
            """)
    long cantidadTomadaPorCliente(@Param("usuarioId") Long usuarioId, @Param("eventoId") Long eventoId);

    /**
     * Confirma la reserva solo si sigue PENDIENTE y no vencio. Es la otra mitad del
     * control de concurrencia: si el job de vencimiento y la confirmacion del pago corren
     * a la vez, solo uno de los dos UPDATEs condicionales gana.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Reserva r set r.estado = com.ticketya.reservas.model.EstadoReserva.CONFIRMADA, r.finalizadaEn = :ahora
             where r.id = :id and r.estado = com.ticketya.reservas.model.EstadoReserva.PENDIENTE and r.expiraEn > :ahora
            """)
    int confirmarSiVigente(@Param("id") Long id, @Param("ahora") LocalDateTime ahora);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Reserva r set r.estado = :nuevo, r.finalizadaEn = :ahora
             where r.id = :id and r.estado = com.ticketya.reservas.model.EstadoReserva.PENDIENTE
            """)
    int finalizarSiPendiente(@Param("id") Long id, @Param("nuevo") EstadoReserva nuevo,
                             @Param("ahora") LocalDateTime ahora);
}
