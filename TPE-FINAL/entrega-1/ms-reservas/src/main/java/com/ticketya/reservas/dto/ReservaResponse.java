package com.ticketya.reservas.dto;

import com.ticketya.reservas.model.EstadoReserva;
import com.ticketya.reservas.model.Reserva;
import com.ticketya.reservas.model.TipoSector;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record ReservaResponse(
        @Schema(example = "15") Long id,
        @Schema(example = "100") Long usuarioId,
        @Schema(example = "1") Long eventoId,
        @Schema(example = "Los Piojos - Ritual 2026") String nombreEvento,
        @Schema(example = "2026-12-15T21:00:00") LocalDateTime fechaEvento,
        @Schema(example = "3") Long sectorId,
        @Schema(example = "Platea Baja") String sector,
        @Schema(example = "NUMERADO") TipoSector tipoSector,
        @Schema(example = "2") Integer cantidad,
        @Schema(example = "45000.00") BigDecimal precioUnitario,
        @Schema(example = "90000.00") BigDecimal montoTotal,
        @Schema(example = "PENDIENTE") EstadoReserva estado,
        @Schema(example = "2026-10-20T18:30:00") LocalDateTime creadaEn,
        @Schema(example = "2026-10-20T18:40:00") LocalDateTime expiraEn,
        @Schema(example = "540", description = "Segundos que faltan para que venza (0 si no esta pendiente)")
        long segundosRestantes,
        List<Item> items) {

    public record Item(@Schema(example = "57") Long asientoId,
                       @Schema(example = "C") String fila,
                       @Schema(example = "12") Integer numero) {
    }

    public static ReservaResponse de(Reserva r, String nombreEvento, LocalDateTime fechaEvento, String sector) {
        long restantes = r.getEstado() == EstadoReserva.PENDIENTE
                ? Math.max(0, Duration.between(LocalDateTime.now(), r.getExpiraEn()).toSeconds())
                : 0;
        return new ReservaResponse(r.getId(), r.getUsuarioId(), r.getEventoId(), nombreEvento, fechaEvento,
                r.getSectorId(), sector, r.getTipoSector(), r.getCantidad(), r.getPrecioUnitario(),
                r.getMontoTotal(), r.getEstado(), r.getCreadaEn(), r.getExpiraEn(), restantes,
                r.getItems().stream().map(i -> new Item(i.getAsientoId(), i.getFila(), i.getNumero())).toList());
    }
}
