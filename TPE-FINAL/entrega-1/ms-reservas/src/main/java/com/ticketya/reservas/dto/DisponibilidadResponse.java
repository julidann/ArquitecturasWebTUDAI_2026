package com.ticketya.reservas.dto;

import com.ticketya.reservas.model.TipoSector;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DisponibilidadResponse(
        @Schema(example = "1") Long eventoId,
        @Schema(example = "Los Piojos - Ritual 2026") String nombreEvento,
        @Schema(example = "2026-12-15T21:00:00") LocalDateTime fechaEvento,
        @Schema(example = "true") boolean habilitado,
        List<SectorDisponible> sectores) {

    public record SectorDisponible(
            @Schema(example = "3") Long sectorId,
            @Schema(example = "Platea Baja") String nombre,
            @Schema(example = "NUMERADO") TipoSector tipo,
            @Schema(example = "45000.00") BigDecimal precio,
            @Schema(example = "200") Integer capacidad,
            @Schema(example = "137") Integer disponibles) {
    }
}
