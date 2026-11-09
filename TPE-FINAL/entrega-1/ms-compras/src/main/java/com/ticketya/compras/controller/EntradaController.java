package com.ticketya.compras.controller;

import com.ticketya.compras.dto.DevolucionRequest;
import com.ticketya.compras.dto.EntradaResponse;
import com.ticketya.compras.dto.SolicitudDevolucionResponse;
import com.ticketya.compras.exception.ErrorResponse;
import com.ticketya.compras.service.DevolucionService;
import com.ticketya.compras.service.EntradaService;
import com.ticketya.compras.service.QrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/entradas")
@Tag(name = "Entradas", description = "Entradas emitidas, su código QR y pedidos de devolución")
public class EntradaController {

    private final EntradaService entradaService;
    private final DevolucionService devolucionService;
    private final QrService qrService;

    public EntradaController(EntradaService entradaService, DevolucionService devolucionService, QrService qrService) {
        this.entradaService = entradaService;
        this.devolucionService = devolucionService;
        this.qrService = qrService;
    }

    @GetMapping
    @Operation(summary = "Mis entradas")
    public List<EntradaResponse> mias(@RequestHeader("X-Usuario-Id") Long usuarioId) {
        return entradaService.deUsuario(usuarioId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una entrada")
    @ApiResponse(responseCode = "403", description = "La entrada es de otro usuario",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EntradaResponse obtener(@PathVariable Long id, @RequestHeader("X-Usuario-Id") Long usuarioId) {
        return entradaService.obtener(id, usuarioId);
    }

    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Imagen PNG del código QR de la entrada")
    @ApiResponse(responseCode = "200", description = "Imagen PNG", content = @Content(mediaType = "image/png"))
    public ResponseEntity<byte[]> qr(@PathVariable Long id, @RequestHeader("X-Usuario-Id") Long usuarioId) {
        EntradaResponse entrada = entradaService.obtener(id, usuarioId);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qrService.generarPng(entrada.codigoQr()));
    }

    @PostMapping("/{id}/devolucion")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Solicitar la devolución de una entrada",
            description = "Se puede pedir hasta 72 horas antes del evento. Queda PENDIENTE hasta que un administrador la resuelva.")
    @ApiResponse(responseCode = "201", description = "Solicitud creada")
    @ApiResponse(responseCode = "403", description = "La entrada es de otro usuario",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La entrada no está válida o ya tiene una solicitud pendiente",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "Faltan menos de 72 horas para el evento",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public SolicitudDevolucionResponse solicitarDevolucion(@PathVariable Long id,
                                                           @RequestHeader("X-Usuario-Id") Long usuarioId,
                                                           @Valid @RequestBody(required = false) DevolucionRequest body) {
        return devolucionService.solicitar(id, usuarioId, body == null ? null : body.motivo());
    }
}
