package com.ticketya.pagos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PagoRequest(
        @NotNull @Schema(example = "21") Long compraId,
        @NotNull @Schema(example = "100") Long usuarioId,
        @NotNull @DecimalMin("0.01") @Schema(example = "90000.00") BigDecimal monto,
        @NotNull @Valid DatosMedioPago medioPago) {
}
