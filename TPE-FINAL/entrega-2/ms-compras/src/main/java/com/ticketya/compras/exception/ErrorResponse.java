package com.ticketya.compras.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Formato comun de error de todos los microservicios")
public record ErrorResponse(
        @Schema(example = "2026-10-20T18:30:00") LocalDateTime timestamp,
        @Schema(example = "409") int status,
        @Schema(example = "Conflict") String error,
        @Schema(example = "La devolución solo se puede pedir hasta 72 horas antes del evento") String mensaje,
        @Schema(example = "/entradas/31/devolucion") String path,
        List<String> detalles) {
}
