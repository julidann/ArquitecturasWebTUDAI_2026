package com.ticketya.usuarios.controller;

import com.ticketya.usuarios.dto.LoginRequest;
import com.ticketya.usuarios.dto.RegistroRequest;
import com.ticketya.usuarios.dto.TokenResponse;
import com.ticketya.usuarios.dto.UsuarioResponse;
import com.ticketya.usuarios.exception.ErrorResponse;
import com.ticketya.usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Registro de clientes e inicio de sesión (públicos)")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrarse como cliente", description = "Público. El usuario queda con rol CLIENTE.")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El email ya está registrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest request) {
        return usuarioService.registrar(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Público. Devuelve el JWT a usar en `Authorization: Bearer`.")
    @ApiResponse(responseCode = "200", description = "Token emitido")
    @ApiResponse(responseCode = "401", description = "Email o contraseña incorrectos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return usuarioService.login(request);
    }
}
