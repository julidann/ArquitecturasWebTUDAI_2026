package com.ticketya.compras.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ResumenAccesoResponse(
        @Schema(example = "1") Long eventoId,
        @Schema(description = "Entradas emitidas que siguen vigentes (válidas + utilizadas)", example = "4500") long entradasVigentes,
        @Schema(description = "Personas que ya ingresaron", example = "3120") long ingresaron,
        @Schema(description = "Escaneos rechazados (QR repetido, de otro evento, devuelto, etc.)", example = "17") long rechazos) {
}
