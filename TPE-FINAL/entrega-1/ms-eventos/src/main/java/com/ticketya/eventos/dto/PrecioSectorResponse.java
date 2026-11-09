package com.ticketya.eventos.dto;

import com.ticketya.eventos.model.PrecioSector;
import com.ticketya.eventos.model.TipoSector;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record PrecioSectorResponse(
        @Schema(example = "3") Long sectorId,
        @Schema(example = "Platea Baja") String sector,
        @Schema(example = "NUMERADO") TipoSector tipo,
        @Schema(example = "45000.00") BigDecimal precio) {

    public static PrecioSectorResponse de(PrecioSector p) {
        return new PrecioSectorResponse(p.getSector().getId(), p.getSector().getNombre(), p.getSector().getTipo(),
                p.getPrecio());
    }
}
