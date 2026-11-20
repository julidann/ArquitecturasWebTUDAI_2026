package com.ticketya.eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record LugarRequest(
        @NotBlank @Schema(example = "Teatro del Lago") String nombre,
        @NotBlank @Schema(example = "Av. Colón 1234") String direccion,
        @NotBlank @Schema(example = "Tandil") String ciudad,
        @Schema(example = "-37.3217") Double latitud,
        @Schema(example = "-59.1332") Double longitud,
        @NotEmpty @Valid List<SectorRequest> sectores) {
}
