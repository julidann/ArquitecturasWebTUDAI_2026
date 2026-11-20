package com.ticketya.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record TokenResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String token,
        @Schema(example = "Bearer") String tipo,
        @Schema(example = "2026-11-20T03:00:00Z") Instant expiraEn,
        UsuarioResponse usuario) {
}
