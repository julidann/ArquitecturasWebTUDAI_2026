package com.ticketya.eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CambioEstadoRequest(
        @Schema(example = "El artista suspendió la gira por problemas de salud") String motivo) {
}
