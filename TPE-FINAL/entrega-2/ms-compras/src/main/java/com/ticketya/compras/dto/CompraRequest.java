package com.ticketya.compras.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CompraRequest(
        @NotNull @Schema(example = "15", description = "Reserva PENDIENTE obtenida en POST /reservas") Long reservaId,
        @NotNull @Valid DatosMedioPago medioPago) {
}
