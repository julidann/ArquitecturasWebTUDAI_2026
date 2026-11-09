package com.ticketya.eventos.dto;

import com.ticketya.eventos.model.TipoSector;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Sector de un lugar. Para NUMERADO se indican filas y asientos por fila; para NO_NUMERADO la capacidad.")
public record SectorRequest(
        @NotBlank @Schema(example = "Platea Baja") String nombre,
        @NotNull @Schema(example = "NUMERADO") TipoSector tipo,
        @Min(1) @Schema(example = "5000", description = "Solo para NO_NUMERADO") Integer capacidad,
        @Min(1) @Schema(example = "10", description = "Solo para NUMERADO") Integer filas,
        @Min(1) @Schema(example = "20", description = "Solo para NUMERADO") Integer asientosPorFila) {
}
