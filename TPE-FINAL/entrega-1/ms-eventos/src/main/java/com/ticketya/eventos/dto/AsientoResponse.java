package com.ticketya.eventos.dto;

import com.ticketya.eventos.model.Asiento;
import io.swagger.v3.oas.annotations.media.Schema;

public record AsientoResponse(
        @Schema(example = "57") Long id,
        @Schema(example = "C") String fila,
        @Schema(example = "12") Integer numero) {

    public static AsientoResponse de(Asiento a) {
        return new AsientoResponse(a.getId(), a.getFila(), a.getNumero());
    }
}
