package com.ticketya.compras.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record ResolucionRequest(
        @Size(max = 500) @Schema(example = "La entrada fue transferida, no corresponde devolución") String observacion) {
}
