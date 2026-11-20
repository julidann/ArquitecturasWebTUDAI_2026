package com.ticketya.compras.repository;

import com.ticketya.compras.model.Entrada;
import com.ticketya.compras.model.EstadoEntrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {

    List<Entrada> findByUsuarioIdOrderByEmitidaEnDesc(Long usuarioId);

    List<Entrada> findByEventoIdAndEstado(Long eventoId, EstadoEntrada estado);

    Optional<Entrada> findByCodigoQr(String codigoQr);

    List<Entrada> findByEventoId(Long eventoId);

    /**
     * Control de acceso (funcionalidad 8): marca la entrada como UTILIZADA solo si existe, es de ese
     * evento y sigue VALIDA. Es un UPDATE condicional atomico, asi que si el mismo QR se escanea en dos
     * puertas a la vez solo una de las dos ve 1 fila afectada (un QR no puede ingresar dos veces).
     */
    @Modifying
    @Query("update Entrada e set e.estado = com.ticketya.compras.model.EstadoEntrada.UTILIZADA, e.utilizadaEn = :ahora, "
            + "e.puertaIngreso = :puerta where e.codigoQr = :qr and e.eventoId = :eventoId "
            + "and e.estado = com.ticketya.compras.model.EstadoEntrada.VALIDA")
    int marcarUtilizada(@Param("qr") String qr, @Param("eventoId") Long eventoId, @Param("puerta") String puerta,
                        @Param("ahora") LocalDateTime ahora);

    /** Totales por sector y estado para los reportes: [sectorId, estado, cantidad, suma de precios]. */
    @Query("select e.sectorId, e.estado, count(e), sum(e.precio) from Entrada e where e.eventoId = :eventoId "
            + "group by e.sectorId, e.estado")
    List<Object[]> totalesPorSectorYEstado(@Param("eventoId") Long eventoId);

    /** Compradores de un evento (para notificarlos de cambios o cancelaciones). */
    @Query("select distinct e.usuarioId from Entrada e where e.eventoId = :eventoId "
            + "and e.estado in (com.ticketya.compras.model.EstadoEntrada.VALIDA, "
            + "com.ticketya.compras.model.EstadoEntrada.UTILIZADA, com.ticketya.compras.model.EstadoEntrada.CANCELADA)")
    List<Long> compradoresDeEvento(@Param("eventoId") Long eventoId);
}
