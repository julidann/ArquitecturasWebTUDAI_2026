package com.ticketya.reservas.controller;

import com.ticketya.reservas.dto.*;
import com.ticketya.reservas.exception.ErrorResponse;
import com.ticketya.reservas.service.DisponibilidadService;
import com.ticketya.reservas.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/disponibilidad/eventos")
@Tag(name = "Disponibilidad", description = "Inventario de lugares por evento")
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;
    private final ReservaService reservaService;

    public DisponibilidadController(DisponibilidadService disponibilidadService, ReservaService reservaService) {
        this.disponibilidadService = disponibilidadService;
        this.reservaService = reservaService;
    }

    @GetMapping("/{eventoId}")
    @Operation(summary = "Disponibilidad por sector de un evento",
            description = "Lugares libres por sector, calculados en tiempo real (no se cachea).")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "El evento no está publicado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DisponibilidadResponse consultar(@PathVariable Long eventoId) {
        return disponibilidadService.consultar(eventoId);
    }

    @GetMapping("/{eventoId}/sectores/{sectorId}/asientos")
    @Operation(summary = "Mapa de asientos de un sector numerado",
            description = "Estado de cada asiento para el evento: DISPONIBLE, RESERVADO o VENDIDO.")
    @ApiResponse(responseCode = "404", description = "No existe el sector para el evento",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El sector no es numerado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<AsientoEstadoResponse> asientos(@PathVariable Long eventoId, @PathVariable Long sectorId) {
        return disponibilidadService.asientos(eventoId, sectorId);
    }

    @PostMapping("/{eventoId}")
    @Operation(summary = "[Interno] Inicializar inventario de un evento",
            description = "Lo invoca ms-eventos al publicar. Idempotente: 201 si se creó, 200 si ya existía.")
    @ApiResponse(responseCode = "201", description = "Inventario creado")
    @ApiResponse(responseCode = "200", description = "El inventario ya existía")
    public ResponseEntity<Void> inicializar(@PathVariable Long eventoId,
                                            @Valid @RequestBody DisponibilidadInicialRequest request) {
        if (!eventoId.equals(request.eventoId())) {
            throw new IllegalArgumentException("El eventoId de la ruta no coincide con el del cuerpo");
        }
        boolean creado = disponibilidadService.inicializar(request);
        return ResponseEntity.status(creado ? HttpStatus.CREATED : HttpStatus.OK).build();
    }

    @PatchMapping("/{eventoId}/estado")
    @Operation(summary = "[Interno] Habilitar o deshabilitar la venta",
            description = "Lo invoca ms-eventos al suspender, reanudar o cancelar. Al deshabilitar se liberan las "
                    + "reservas pendientes.")
    @ApiResponse(responseCode = "204", description = "Actualizado")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Long eventoId,
                                              @Valid @RequestBody HabilitacionRequest request) {
        disponibilidadService.cambiarHabilitacion(eventoId, request.habilitado());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventoId}/liberaciones")
    @Operation(summary = "[Interno] Liberar un lugar vendido (devolución)",
            description = "Lo invoca ms-compras cuando un administrador aprueba una devolución. Requiere "
                    + "Idempotency-Key: un reintento con la misma clave no libera el lugar dos veces.")
    @ApiResponse(responseCode = "204", description = "Lugar liberado (o ya estaba liberado con esa clave)")
    @ApiResponse(responseCode = "409", description = "La reserva no corresponde a una venta del evento",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> liberar(@PathVariable Long eventoId, @Valid @RequestBody LiberacionRequest request,
                                        @RequestHeader("Idempotency-Key") String clave) {
        reservaService.liberarVendido(eventoId, request, clave);
        return ResponseEntity.noContent().build();
    }
}
