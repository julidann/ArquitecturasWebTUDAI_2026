package com.ticketya.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "cliente@ticketya.com") @NotBlank String email,
        @Schema(example = "cliente123") @NotBlank String password) {
}
