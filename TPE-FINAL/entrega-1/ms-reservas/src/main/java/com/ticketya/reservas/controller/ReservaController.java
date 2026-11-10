package com.ticketya.reservas.controller;

import com.ticketya.reservas.dto.ReservaRequest;
import com.ticketya.reservas.dto.ReservaResponse;
import com.ticketya.reservas.exception.AccesoDenegadoException;
import com.ticketya.reservas.exception.ErrorResponse;
import com.ticketya.reservas.model.EstadoReserva;
import com.ticketya.reservas.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservas")
@Tag(name = "Reservas", description = "Reserva temporal de lugares mientras el cliente paga")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reservar lugares",
            description = "Bloquea los asientos elegidos (sector numerado) o una cantidad de lugares (sector no numerado) "
                    + "durante 10 minutos. Es todo o nada: si alguno de los asientos ya no está libre no se reserva ninguno.")
    @ApiResponse(responseCode = "201", description = "Reserva creada en estado PENDIENTE")
    @ApiResponse(responseCode = "400", description = "Pedido inválido (ej: asientos en un sector no numerado)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Evento, sector o asiento inexistente",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Lugar no disponible o evento sin venta habilitada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "Supera el límite de entradas por cliente",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ReservaResponse crear(@Valid @RequestBody ReservaRequest request,
                                 @Parameter(example = "100") @RequestHeader("X-Usuario-Id") Long usuarioId) {
        return reservaService.crear(usuarioId, request);
    }

    @GetMapping
    @Operation(summary = "Mis reservas", description = "Reservas del usuario del header, opcionalmente por estado.")
    public List<ReservaResponse> mias(@RequestHeader("X-Usuario-Id") Long usuarioId,
                                      @RequestParam(required = false) EstadoReserva estado) {
        return reservaService.deUsuario(usuarioId, estado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una reserva",
            description = "Si se envía X-Usuario-Id se valida que la reserva sea del usuario. Los servicios internos lo omiten.")
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ReservaResponse obtener(@PathVariable Long id,
                                   @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        ReservaResponse r = reservaService.obtener(id);
        if (usuarioId != null && !usuarioId.equals(r.usuarioId())) {
            throw new AccesoDenegadoException("La reserva no pertenece al usuario");
        }
        return r;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar mi reserva", description = "Libera los lugares antes de que venza.")
    @ApiResponse(responseCode = "403", description = "La reserva es de otro usuario",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La reserva ya fue confirmada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ReservaResponse cancelar(@PathVariable Long id, @RequestHeader("X-Usuario-Id") Long usuarioId) {
        return reservaService.cancelarPorCliente(id, usuarioId);
    }

    @PostMapping("/{id}/confirmar")
    @Operation(summary = "[Interno] Confirmar reserva (pago aprobado)",
            description = "Lo invoca ms-compras. Pasa los lugares a VENDIDO. Idempotente.")
    @ApiResponse(responseCode = "200", description = "Reserva confirmada")
    @ApiResponse(responseCode = "409", description = "La reserva venció o fue liberada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ReservaResponse confirmar(@PathVariable Long id) {
        return reservaService.confirmar(id);
    }

    @PostMapping("/{id}/liberar")
    @Operation(summary = "[Interno] Liberar reserva (pago rechazado)",
            description = "Lo invoca ms-compras. Devuelve los lugares al inventario. Idempotente.")
    @ApiResponse(responseCode = "409", description = "La reserva ya fue confirmada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ReservaResponse liberar(@PathVariable Long id) {
        return reservaService.liberar(id, EstadoReserva.LIBERADA);
    }
}
