package com.ticketya.eventos.dto;

import com.ticketya.eventos.model.Sector;
import com.ticketya.eventos.model.TipoSector;
import io.swagger.v3.oas.annotations.media.Schema;

public record SectorResponse(
        @Schema(example = "3") Long id,
        @Schema(example = "Platea Baja") String nombre,
        @Schema(example = "NUMERADO") TipoSector tipo,
        @Schema(example = "200") Integer capacidad) {

    public static SectorResponse de(Sector s) {
        return new SectorResponse(s.getId(), s.getNombre(), s.getTipo(), s.getCapacidad());
    }
}
