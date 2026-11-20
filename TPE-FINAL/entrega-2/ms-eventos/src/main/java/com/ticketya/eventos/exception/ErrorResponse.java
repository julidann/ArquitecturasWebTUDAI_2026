package com.ticketya.eventos.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Formato comun de error de todos los microservicios")
public record ErrorResponse(
        @Schema(example = "2026-10-20T18:30:00") LocalDateTime timestamp,
        @Schema(example = "409") int status,
        @Schema(example = "Conflict") String error,
        @Schema(example = "El evento no tiene precio definido para todos los sectores") String mensaje,
        @Schema(example = "/eventos/1/publicar") String path,
        List<String> detalles) {
}
