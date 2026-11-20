package com.ticketya.compras.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record DevolucionRequest(
        @Size(max = 500) @Schema(example = "No puedo asistir por un viaje de trabajo") String motivo) {
}
