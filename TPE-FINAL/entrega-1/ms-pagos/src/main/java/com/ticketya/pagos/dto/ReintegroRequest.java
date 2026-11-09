package com.ticketya.pagos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ReintegroRequest(
        @NotNull @DecimalMin("0.01") @Schema(example = "45000.00") BigDecimal monto,
        @Schema(example = "Devolución aprobada de la entrada 31") String motivo) {
}
