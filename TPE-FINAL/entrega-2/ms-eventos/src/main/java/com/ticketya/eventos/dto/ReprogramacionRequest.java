package com.ticketya.eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReprogramacionRequest(
        @NotNull @Future @Schema(example = "2027-01-20T21:00:00") LocalDateTime fechaHora,
        @Schema(example = "Lluvias: se pasa al mes siguiente") String motivo) {
}
