package com.ticketya.recomendaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PreferenciasRequest(
        @Size(max = 10) @Schema(example = "[\"RECITAL\", \"TEATRO\"]") List<String> tiposEvento,
        @Size(max = 30) @Schema(example = "[\"Los Piojos\", \"Fito Paez\"]") List<String> artistasFavoritos,
        @Size(max = 80) @Schema(example = "Tandil") String ciudad) {
}
