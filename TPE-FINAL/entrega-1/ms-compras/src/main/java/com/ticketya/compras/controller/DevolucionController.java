package com.ticketya.compras.controller;

import com.ticketya.compras.dto.ResolucionRequest;
import com.ticketya.compras.dto.SolicitudDevolucionResponse;
import com.ticketya.compras.exception.ErrorResponse;
import com.ticketya.compras.model.EstadoSolicitud;
import com.ticketya.compras.model.Rol;
import com.ticketya.compras.service.DevolucionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/devoluciones")
@Tag(name = "Devoluciones", description = "Resolución de solicitudes de devolución (administradores)")
public class DevolucionController {

    private final DevolucionService devolucionService;

    public DevolucionController(DevolucionService devolucionService) {
        this.devolucionService = devolucionService;
    }

    @GetMapping
    @Operation(summary = "Listar solicitudes de devolución",
            description = "Un ADMIN ve todas; cualquier otro rol ve solo las propias.")
    public List<SolicitudDevolucionResponse> listar(
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @Parameter(example = "ADMIN") @RequestHeader(value = "X-Rol", required = false) String rol,
            @Parameter(example = "PENDIENTE") @RequestParam(required = false) EstadoSolicitud estado) {
        return devolucionService.listar(usuarioId, Rol.desde(rol), estado);
    }

    @PostMapping("/{id}/aprobar")
    @Operation(summary = "Aprobar una devolución (ADMIN)",
            description = "Libera el lugar en ms-reservas y pide el reintegro del precio de la entrada a ms-pagos.")
    @ApiResponse(responseCode = "200", description = "Devolución aprobada")
    @ApiResponse(responseCode = "403", description = "No es administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La solicitud ya fue resuelta",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "ms-reservas o ms-pagos no disponibles (se puede reintentar)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public SolicitudDevolucionResponse aprobar(@PathVariable Long id,
                                               @Parameter(example = "1") @RequestHeader("X-Usuario-Id") Long adminId,
                                               @Parameter(example = "ADMIN") @RequestHeader(value = "X-Rol", required = false) String rol) {
        return devolucionService.aprobar(id, adminId, Rol.desde(rol));
    }

    @PostMapping("/{id}/rechazar")
    @Operation(summary = "Rechazar una devolución (ADMIN)")
    @ApiResponse(responseCode = "403", description = "No es administrador",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La solicitud ya fue resuelta",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public SolicitudDevolucionResponse rechazar(@PathVariable Long id,
                                                @RequestHeader("X-Usuario-Id") Long adminId,
                                                @RequestHeader(value = "X-Rol", required = false) String rol,
                                                @Valid @RequestBody(required = false) ResolucionRequest body) {
        return devolucionService.rechazar(id, adminId, Rol.desde(rol), body == null ? null : body.observacion());
    }
}
