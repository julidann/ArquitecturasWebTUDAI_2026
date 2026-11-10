package com.ticketya.reservas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Libera un lugar ya vendido (por una devolucion aprobada)")
public record LiberacionRequest(
        @NotNull @Schema(example = "15") Long reservaId,
        @NotNull @Schema(example = "3") Long sectorId,
        @Schema(example = "57", description = "Solo para sectores numerados") Long asientoId,
        @NotNull @Min(1) @Schema(example = "1") Integer cantidad) {
}
