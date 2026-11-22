package com.ticketya.fila.controller;

import com.ticketya.fila.dto.ConfiguracionRequest;
import com.ticketya.fila.dto.EstadoFilaResponse;
import com.ticketya.fila.dto.TurnoResponse;
import com.ticketya.fila.exception.ErrorResponse;
import com.ticketya.fila.service.FilaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fila/eventos/{eventoId}")
@Tag(name = "Fila virtual")
public class FilaController {

    private final FilaService filaService;

    public FilaController(FilaService filaService) {
        this.filaService = filaService;
    }

    @PostMapping("/ingresar")
    @Operation(summary = "Ingresar a la fila (CLIENTE)",
            description = "Idempotente: si ya estaba en la fila conserva su lugar; si ya estaba habilitado devuelve su turno.")
    @ApiResponse(responseCode = "200", description = "Posición en la fila o turno habilitado")
    @ApiResponse(responseCode = "404", description = "El evento no tiene fila virtual",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La fila está cerrada (evento cancelado)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TurnoResponse ingresar(@PathVariable long eventoId,
                                  @Parameter(hidden = true) @RequestHeader("X-Usuario-Id") long usuarioId) {
        return filaService.ingresar(eventoId, usuarioId);
    }

    @GetMapping("/mi-turno")
    @Operation(summary = "Consultar mi posición / turno (CLIENTE)",
            description = "La app lo consulta cada algunos segundos. Cuando el estado pasa a HABILITADO trae el `turno`.")
    @ApiResponse(responseCode = "404", description = "No está en la fila o su turno venció",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TurnoResponse miTurno(@PathVariable long eventoId,
                                 @Parameter(hidden = true) @RequestHeader("X-Usuario-Id") long usuarioId) {
        return filaService.miTurno(eventoId, usuarioId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Salir de la fila (CLIENTE)")
    public void abandonar(@PathVariable long eventoId,
                          @Parameter(hidden = true) @RequestHeader("X-Usuario-Id") long usuarioId) {
        filaService.abandonar(eventoId, usuarioId);
    }

    @GetMapping("/estado")
    @Operation(summary = "Estado de la fila (ORGANIZADOR dueño o ADMIN)")
    public EstadoFilaResponse estado(@PathVariable long eventoId,
                                     @Parameter(hidden = true) @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
                                     @Parameter(hidden = true) @RequestHeader(value = "X-Rol", required = false) String rol) {
        return filaService.estado(eventoId, usuarioId, rol);
    }

    @PutMapping("/configuracion")
    @Operation(summary = "Configurar tandas (ORGANIZADOR dueño o ADMIN)",
            description = "Tamaño de tanda, segundos entre tandas y minutos que tiene un habilitado para comprar. "
                    + "Un ADMIN puede abrir a mano la fila de un evento que no la tenía.")
    public EstadoFilaResponse configurar(@PathVariable long eventoId, @Valid @RequestBody ConfiguracionRequest request,
                                         @Parameter(hidden = true) @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
                                         @Parameter(hidden = true) @RequestHeader(value = "X-Rol", required = false) String rol) {
        return filaService.configurar(eventoId, request, usuarioId, rol);
    }
}
