package com.ticketya.usuarios.dto;

import com.ticketya.usuarios.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta hecha por un administrador: permite elegir el rol (organizador, personal de acceso, otro admin). */
public record AltaUsuarioRequest(
        @Schema(example = "puerta3@ticketya.com") @NotBlank @Email String email,
        @Schema(example = "Puerta 3 - Estadio Único") @NotBlank @Size(max = 120) String nombre,
        @Schema(example = "acceso123") @NotBlank @Size(min = 6, max = 72) String password,
        @Schema(example = "ACCESO") @NotNull Rol rol) {
}
