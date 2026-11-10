package com.ticketya.reservas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Para sectores NUMERADOS se envian los asientoIds; para NO_NUMERADOS la cantidad.")
public record ReservaRequest(
        @NotNull @Schema(example = "1") Long eventoId,
        @NotNull @Schema(example = "3") Long sectorId,
        @Schema(example = "[57, 58]") List<Long> asientoIds,
        @Min(1) @Schema(example = "2") Integer cantidad) {
}
