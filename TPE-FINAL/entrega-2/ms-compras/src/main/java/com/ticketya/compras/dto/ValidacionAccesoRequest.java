package com.ticketya.compras.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ValidacionAccesoRequest(
        @Schema(description = "Contenido del QR escaneado", example = "1f0b6c2e-8f5a-4d7e-9a43-2b1d7c9e0a11") @NotBlank String codigoQr,
        @Schema(description = "Evento en el que está trabajando el lector", example = "1") @NotNull Long eventoId,
        @Schema(example = "Puerta 3") String puerta) {
}
