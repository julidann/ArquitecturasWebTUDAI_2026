package com.ticketya.eventos.controller;

import com.ticketya.eventos.dto.*;
import com.ticketya.eventos.exception.ErrorResponse;
import com.ticketya.eventos.model.Rol;
import com.ticketya.eventos.model.TipoEvento;
import com.ticketya.eventos.service.BusquedaService;
import com.ticketya.eventos.service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/eventos")
@Tag(name = "Eventos", description = "Alta, publicación, cancelación y búsqueda de eventos")
public class EventoController {

    private final EventoService eventoService;
    private final BusquedaService busquedaService;

    public EventoController(EventoService eventoService, BusquedaService busquedaService) {
        this.eventoService = eventoService;
        this.busquedaService = busquedaService;
    }

    @GetMapping
    @Operation(summary = "Buscar eventos",
            description = "Búsqueda por múltiples criterios combinables. Solo devuelve eventos PUBLICADOS o AGOTADOS. "
                    + "Orden: `fecha` (default), `precio` (precio mínimo del evento) o `relevancia` "
                    + "(coincidencias del texto `q` en nombre, participantes y descripción).")
    @ApiResponse(responseCode = "200", description = "Página de resultados")
    @ApiResponse(responseCode = "400", description = "Parámetros inválidos (ej: orden desconocido o rango de fechas invertido)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PaginaResponse<EventoResponse> buscar(
            @Parameter(description = "Texto libre", example = "piojos") @RequestParam(required = false) String q,
            @Parameter(example = "RECITAL") @RequestParam(required = false) TipoEvento tipo,
            @Parameter(example = "10") @RequestParam(required = false) Long organizadorId,
            @Parameter(example = "Los Piojos") @RequestParam(required = false) String participante,
            @Parameter(example = "La Plata") @RequestParam(required = false) String ciudad,
            @Parameter(example = "2026-12-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(example = "2026-12-31") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "fecha | precio | relevancia", example = "fecha") @RequestParam(required = false) String orden,
            @Parameter(description = "asc | desc", example = "asc") @RequestParam(defaultValue = "asc") String direccion,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        FiltroBusqueda filtro = new FiltroBusqueda(q, tipo, organizadorId, participante, ciudad, desde, hasta,
                OrdenBusqueda.desde(orden), "desc".equalsIgnoreCase(direccion), pagina, tamanio);
        return busquedaService.buscar(filtro);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear evento (queda en BORRADOR)",
            description = "El organizador es el usuario del header X-Usuario-Id. Requiere rol ORGANIZADOR o ADMIN.")
    @ApiResponse(responseCode = "201", description = "Evento creado en borrador")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Rol no autorizado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No existe el lugar",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EventoResponse crear(@Valid @RequestBody EventoRequest request,
                                @Parameter(example = "10") @RequestHeader("X-Usuario-Id") Long usuarioId,
                                @Parameter(example = "ORGANIZADOR") @RequestHeader(value = "X-Rol", required = false) String rol) {
        return eventoService.crear(request, usuarioId, Rol.desde(rol));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un evento")
    @ApiResponse(responseCode = "404", description = "No existe el evento",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EventoResponse obtener(@PathVariable Long id) {
        return eventoService.obtener(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificar un evento en borrador")
    @ApiResponse(responseCode = "403", description = "No es el organizador del evento",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El evento no está en borrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EventoResponse actualizar(@PathVariable Long id, @Valid @RequestBody EventoRequest request,
                                     @RequestHeader("X-Usuario-Id") Long usuarioId,
                                     @RequestHeader(value = "X-Rol", required = false) String rol) {
        return eventoService.actualizar(id, request, usuarioId, Rol.desde(rol));
    }

    @PutMapping("/{id}/precios")
    @Operation(summary = "Definir el precio de cada sector",
            description = "Se puede llamar varias veces mientras el evento está en borrador; los precios se pisan por sector.")
    @ApiResponse(responseCode = "409", description = "Sector ajeno al lugar o evento no está en borrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EventoResponse definirPrecios(@PathVariable Long id, @Valid @RequestBody PreciosRequest request,
                                         @RequestHeader("X-Usuario-Id") Long usuarioId,
                                         @RequestHeader(value = "X-Rol", required = false) String rol) {
        return eventoService.definirPrecios(id, request, usuarioId, Rol.desde(rol));
    }

    @PostMapping("/{id}/publicar")
    @Operation(summary = "Publicar un evento",
            description = "Requiere que todos los sectores del lugar tengan precio. Al publicar se crea el inventario "
                    + "de lugares en ms-reservas y el evento queda visible en las búsquedas.")
    @ApiResponse(responseCode = "200", description = "Evento publicado")
    @ApiResponse(responseCode = "409", description = "Faltan precios o el evento no está en borrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "502", description = "ms-reservas no respondió; el evento sigue en borrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EventoResponse publicar(@PathVariable Long id, @RequestHeader("X-Usuario-Id") Long usuarioId,
                                   @RequestHeader(value = "X-Rol", required = false) String rol) {
        return eventoService.publicar(id, usuarioId, Rol.desde(rol));
    }

    @PostMapping("/{id}/suspender")
    @Operation(summary = "Suspender un evento (solo ADMIN)", description = "Frena las ventas sin cancelar las entradas ya vendidas.")
    @ApiResponse(responseCode = "403", description = "No es administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EventoResponse suspender(@PathVariable Long id, @RequestBody(required = false) CambioEstadoRequest body,
                                    @RequestHeader(value = "X-Rol", required = false) String rol) {
        return eventoService.suspender(id, body == null ? null : body.motivo(), Rol.desde(rol));
    }

    @PostMapping("/{id}/reanudar")
    @Operation(summary = "Reanudar un evento suspendido (solo ADMIN)")
    public EventoResponse reanudar(@PathVariable Long id, @RequestHeader(value = "X-Rol", required = false) String rol) {
        return eventoService.reanudar(id, Rol.desde(rol));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar un evento (solo ADMIN)",
            description = "Deshabilita la venta y todas las entradas vendidas pasan a reintegro (ms-compras solicita "
                    + "el reintegro a la pasarela de pagos). Es idempotente.")
    @ApiResponse(responseCode = "403", description = "No es administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El evento ya finalizó",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EventoResponse cancelar(@PathVariable Long id, @RequestBody(required = false) CambioEstadoRequest body,
                                   @RequestHeader(value = "X-Rol", required = false) String rol) {
        return eventoService.cancelar(id, body == null ? null : body.motivo(), Rol.desde(rol));
    }
}
