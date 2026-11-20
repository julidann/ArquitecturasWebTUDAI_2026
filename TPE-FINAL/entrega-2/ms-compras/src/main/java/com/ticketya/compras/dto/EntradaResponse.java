package com.ticketya.compras.dto;

import com.ticketya.compras.model.Entrada;
import com.ticketya.compras.model.EstadoEntrada;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EntradaResponse(
        @Schema(example = "31") Long id,
        @Schema(example = "21") Long compraId,
        @Schema(example = "1") Long eventoId,
        @Schema(example = "Los Piojos - Ritual 2026") String nombreEvento,
        @Schema(example = "2026-12-15T21:00:00") LocalDateTime fechaEvento,
        @Schema(example = "3") Long sectorId,
        @Schema(example = "Platea Baja") String sector,
        @Schema(example = "57") Long asientoId,
        @Schema(example = "C") String fila,
        @Schema(example = "12") Integer numero,
        @Schema(example = "45000.00") BigDecimal precio,
        @Schema(example = "6f1c2a9e-3d4b-4f7a-9c1e-2b8d5e0a7f31") String codigoQr,
        @Schema(example = "VALIDA") EstadoEntrada estado,
        @Schema(example = "2026-10-20T18:31:06") LocalDateTime emitidaEn) {

    public static EntradaResponse de(Entrada e) {
        return new EntradaResponse(e.getId(), e.getCompra().getId(), e.getEventoId(), e.getCompra().getNombreEvento(),
                e.getCompra().getFechaEvento(), e.getSectorId(), e.getCompra().getSectorNombre(), e.getAsientoId(),
                e.getFila(), e.getNumero(), e.getPrecio(), e.getCodigoQr(), e.getEstado(), e.getEmitidaEn());
    }
}
