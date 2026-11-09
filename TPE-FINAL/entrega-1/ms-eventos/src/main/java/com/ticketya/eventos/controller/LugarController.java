package com.ticketya.eventos.controller;

import com.ticketya.eventos.dto.AsientoResponse;
import com.ticketya.eventos.dto.LugarRequest;
import com.ticketya.eventos.dto.LugarResponse;
import com.ticketya.eventos.exception.AccesoDenegadoException;
import com.ticketya.eventos.exception.ErrorResponse;
import com.ticketya.eventos.model.Rol;
import com.ticketya.eventos.service.LugarService;
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
@RequestMapping("/lugares")
@Tag(name = "Lugares", description = "Estadios, teatros y salas con sus sectores")
public class LugarController {

    private final LugarService lugarService;

    public LugarController(LugarService lugarService) {
        this.lugarService = lugarService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un lugar",
            description = "Da de alta un lugar con sus sectores. Para los sectores numerados se generan los asientos "
                    + "automáticamente (filas A, B, C... y números 1..N). Requiere rol ORGANIZADOR o ADMIN.")
    @ApiResponse(responseCode = "201", description = "Lugar creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El rol no puede crear lugares",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public LugarResponse crear(@Valid @RequestBody LugarRequest request,
                               @Parameter(example = "ORGANIZADOR") @RequestHeader(value = "X-Rol", required = false) String rol) {
        Rol r = Rol.desde(rol);
        if (r != Rol.ORGANIZADOR && r != Rol.ADMIN) {
            throw new AccesoDenegadoException("Solo organizadores o administradores pueden crear lugares");
        }
        return lugarService.crear(request);
    }

    @GetMapping
    @Operation(summary = "Listar lugares", description = "Opcionalmente filtra por ciudad.")
    public List<LugarResponse> listar(@Parameter(example = "Tandil") @RequestParam(required = false) String ciudad) {
        return lugarService.listar(ciudad);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un lugar con sus sectores")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "No existe el lugar",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public LugarResponse obtener(@PathVariable Long id) {
        return lugarService.obtener(id);
    }

    @GetMapping("/{id}/sectores/{sectorId}/asientos")
    @Operation(summary = "Listar los asientos de un sector numerado",
            description = "Devuelve la grilla de asientos del lugar (sin estado). El estado de cada asiento para un "
                    + "evento se consulta en ms-reservas: GET /disponibilidad/eventos/{eventoId}/sectores/{sectorId}/asientos")
    @ApiResponse(responseCode = "404", description = "No existe el lugar o el sector",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<AsientoResponse> asientos(@PathVariable Long id, @PathVariable Long sectorId) {
        return lugarService.asientosDeSector(id, sectorId);
    }
}
