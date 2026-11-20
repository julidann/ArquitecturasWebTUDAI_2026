package com.ticketya.usuarios.controller;

import com.ticketya.usuarios.dto.AltaUsuarioRequest;
import com.ticketya.usuarios.dto.ContactoResponse;
import com.ticketya.usuarios.dto.UsuarioResponse;
import com.ticketya.usuarios.exception.ErrorResponse;
import com.ticketya.usuarios.model.Rol;
import com.ticketya.usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Usuarios")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/usuarios/me")
    @Operation(summary = "Mis datos", description = "Cualquier usuario autenticado.")
    public UsuarioResponse yo(@Parameter(hidden = true) @RequestHeader("X-Usuario-Id") Long usuarioId) {
        return usuarioService.obtener(usuarioId);
    }

    @GetMapping("/usuarios")
    @Operation(summary = "Listar usuarios (ADMIN)")
    @ApiResponse(responseCode = "403", description = "No es administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<UsuarioResponse> listar(@RequestParam(required = false) Rol rol,
                                        @Parameter(hidden = true) @RequestHeader(value = "X-Rol", required = false) String rolSolicitante) {
        return usuarioService.listar(rol, Rol.desde(rolSolicitante));
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Alta de usuario con rol (ADMIN)",
            description = "Para crear organizadores, personal de acceso u otros administradores.")
    @ApiResponse(responseCode = "403", description = "No es administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El email ya está registrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public UsuarioResponse alta(@Valid @RequestBody AltaUsuarioRequest request,
                                @Parameter(hidden = true) @RequestHeader(value = "X-Rol", required = false) String rol) {
        return usuarioService.alta(request, Rol.desde(rol));
    }

    @PostMapping("/usuarios/{id}/deshabilitar")
    @Operation(summary = "Deshabilitar un usuario (ADMIN)", description = "No puede volver a iniciar sesión.")
    public UsuarioResponse deshabilitar(@PathVariable Long id,
                                        @Parameter(hidden = true) @RequestHeader(value = "X-Rol", required = false) String rol) {
        return usuarioService.cambiarActivo(id, false, Rol.desde(rol));
    }

    @GetMapping("/internal/usuarios/{id}")
    @Operation(summary = "[Interno] Datos de contacto", description = "Lo usa ms-notificaciones para obtener el email.")
    public ContactoResponse contacto(@PathVariable Long id) {
        return usuarioService.contacto(id);
    }
}
