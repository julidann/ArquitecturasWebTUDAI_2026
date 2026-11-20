package com.ticketya.usuarios.dto;

import com.ticketya.usuarios.model.Rol;
import com.ticketya.usuarios.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record UsuarioResponse(
        @Schema(example = "3") Long id,
        @Schema(example = "cliente@ticketya.com") String email,
        @Schema(example = "Carla Cliente") String nombre,
        @Schema(example = "CLIENTE") Rol rol,
        boolean activo,
        LocalDateTime creadoEn) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getEmail(), u.getNombre(), u.getRol(), u.isActivo(), u.getCreadoEn());
    }
}
