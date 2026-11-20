package com.ticketya.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @Schema(example = "juana@mail.com") @NotBlank @Email String email,
        @Schema(example = "Juana Pérez") @NotBlank @Size(max = 120) String nombre,
        @Schema(example = "secreta123") @NotBlank @Size(min = 6, max = 72) String password) {
}
