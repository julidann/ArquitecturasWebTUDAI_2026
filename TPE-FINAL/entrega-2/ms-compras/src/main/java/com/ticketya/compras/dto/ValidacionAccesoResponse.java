package com.ticketya.compras.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ValidacionAccesoResponse(
        @Schema(description = "true: puede pasar", example = "true") boolean permitido,
        @Schema(example = "PERMITIDO") String resultado,
        @Schema(example = "Entrada válida. ¡Bienvenido!") String mensaje,
        @Schema(example = "15") Long entradaId,
        @Schema(example = "Platea") String sector,
        @Schema(example = "C") String fila,
        @Schema(example = "12") Integer numero,
        @Schema(description = "Si ya se había usado: cuándo ingresó") LocalDateTime ingresoAnterior) {
}
