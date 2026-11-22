package com.ticketya.fila.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record EstadoFilaResponse(
        @Schema(example = "7") Long eventoId,
        @Schema(example = "true") boolean activa,
        @Schema(example = "12000") long enEspera,
        @Schema(description = "Clientes habilitados desde que se abrió la fila", example = "3000") long habilitadosTotal,
        @Schema(example = "200") int tamanioTanda,
        @Schema(example = "10") int intervaloSegundos,
        @Schema(example = "10") int minutosParaComprar) {
}
