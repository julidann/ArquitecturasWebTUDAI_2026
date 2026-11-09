package com.ticketya.eventos.dto;

import com.ticketya.eventos.model.Lugar;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record LugarResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Teatro del Lago") String nombre,
        @Schema(example = "Av. Colón 1234") String direccion,
        @Schema(example = "Tandil") String ciudad,
        @Schema(example = "-37.3217") Double latitud,
        @Schema(example = "-59.1332") Double longitud,
        List<SectorResponse> sectores) {

    public static LugarResponse de(Lugar l) {
        return new LugarResponse(l.getId(), l.getNombre(), l.getDireccion(), l.getCiudad(), l.getLatitud(),
                l.getLongitud(), l.getSectores().stream().map(SectorResponse::de).toList());
    }
}
