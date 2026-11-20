package com.ticketya.compras.controller;

import com.ticketya.compras.dto.ReporteEventoResponse;
import com.ticketya.compras.exception.ErrorResponse;
import com.ticketya.compras.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reportes")
@Tag(name = "Reportes", description = "Reportes de ventas para organizadores")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/eventos/{eventoId}")
    @Operation(summary = "Reporte de un evento (ORGANIZADOR dueño o ADMIN)",
            description = "Por sector: vendidas, devueltas, canceladas, vigentes, ingresaron, % de ocupación y "
                    + "recaudación neta. Se calcula en el momento (no se cachea).")
    @ApiResponse(responseCode = "200", description = "Reporte")
    @ApiResponse(responseCode = "403", description = "No es el organizador del evento",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No existe el evento",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ReporteEventoResponse reporte(@PathVariable Long eventoId,
                                         @Parameter(hidden = true) @RequestHeader("X-Usuario-Id") Long usuarioId,
                                         @Parameter(hidden = true) @RequestHeader(value = "X-Rol", required = false) String rol) {
        return reporteService.reporte(eventoId, usuarioId, rol);
    }
}
