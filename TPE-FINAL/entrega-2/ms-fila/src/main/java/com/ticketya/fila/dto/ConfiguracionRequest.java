package com.ticketya.fila.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfiguracionRequest(
        @Schema(description = "Clientes que se habilitan por tanda", example = "200") @NotNull @Min(1) @Max(10000) Integer tamanioTanda,
        @Schema(description = "Segundos entre tandas", example = "10") @NotNull @Min(1) @Max(3600) Integer intervaloSegundos,
        @Schema(description = "Minutos que tiene un cliente habilitado para reservar", example = "10") @NotNull @Min(1) @Max(60) Integer minutosParaComprar,
        @Schema(description = "false cierra la fila (no entra nadie más)", example = "true") Boolean activa) {
}
