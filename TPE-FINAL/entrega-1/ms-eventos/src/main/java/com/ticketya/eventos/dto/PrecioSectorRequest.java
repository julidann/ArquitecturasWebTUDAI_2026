package com.ticketya.eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PrecioSectorRequest(
        @NotNull @Schema(example = "3") Long sectorId,
        @NotNull @DecimalMin("0.0") @Schema(example = "45000.00") BigDecimal precio) {
}
