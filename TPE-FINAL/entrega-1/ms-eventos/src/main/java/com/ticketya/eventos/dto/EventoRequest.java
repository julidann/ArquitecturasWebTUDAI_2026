package com.ticketya.eventos.dto;

import com.ticketya.eventos.model.TipoEvento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record EventoRequest(
        @NotBlank @Schema(example = "Los Piojos - Ritual 2026") String nombre,
        @Schema(example = "Show de regreso en el Estadio Único") String descripcion,
        @NotNull @Schema(example = "RECITAL") TipoEvento tipo,
        @Schema(example = "[\"Los Piojos\"]") List<String> participantes,
        @NotNull @Future @Schema(example = "2026-12-15T21:00:00") LocalDateTime fechaHora,
        @NotNull @Schema(example = "1") Long lugarId,
        @Schema(example = "false", description = "Si es true, en la entrega 2 se usa fila virtual") Boolean altaDemanda,
        @Min(1) @Max(20) @Schema(example = "4", description = "Maximo de entradas por cliente para el evento (default 6)")
        Integer limiteEntradasPorCliente) {
}
