package com.ticketya.reservas.dto;

import com.ticketya.reservas.model.AsientoDisponibilidad;
import com.ticketya.reservas.model.EstadoAsiento;
import io.swagger.v3.oas.annotations.media.Schema;

public record AsientoEstadoResponse(
        @Schema(example = "57") Long asientoId,
        @Schema(example = "C") String fila,
        @Schema(example = "12") Integer numero,
        @Schema(example = "DISPONIBLE") EstadoAsiento estado) {

    public static AsientoEstadoResponse de(AsientoDisponibilidad a) {
        return new AsientoEstadoResponse(a.getAsientoId(), a.getFila(), a.getNumero(), a.getEstado());
    }
}
