package com.ticketya.compras.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CancelacionEventoResponse(
        @Schema(example = "1") Long eventoId,
        @Schema(example = "120") int entradasCanceladas,
        @Schema(example = "120") int reintegrosSolicitados,
        @Schema(example = "0", description = "Reintegros que fallaron; se reprocesan si se vuelve a llamar")
        int reintegrosFallidos,
        @Schema(example = "45") int compradoresNotificados) {
}
