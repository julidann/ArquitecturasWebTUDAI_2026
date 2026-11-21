package com.ticketya.bitacora.controller;

import com.ticketya.bitacora.dto.RegistroResponse;
import com.ticketya.bitacora.service.BitacoraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/bitacora")
@Validated
@Tag(name = "Bitácora", description = "Log centralizado de operaciones críticas (solo ADMIN)")
public class BitacoraController {

    private final BitacoraService service;

    public BitacoraController(BitacoraService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Buscar en la bitácora",
            description = "Todos los filtros son opcionales. `operacion` acepta el nombre completo (compra.aprobada) "
                    + "o el prefijo (compra).")
    public List<RegistroResponse> buscar(@RequestParam(required = false) String correlationId,
                                         @RequestParam(required = false) String operacion,
                                         @RequestParam(required = false) String servicio,
                                         @RequestParam(required = false) Long usuarioId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
                                         @RequestParam(defaultValue = "50") @Min(1) @Max(500) int limite) {
        return service.buscar(new BitacoraService.Filtro(correlationId, operacion, servicio, usuarioId, desde, hasta,
                limite));
    }

    @GetMapping("/trazas/{correlationId}")
    @Operation(summary = "Seguir una operación entre servicios",
            description = "Devuelve, en orden, todo lo que registraron los distintos microservicios con ese "
                    + "X-Correlation-Id (ej. la compra: reserva confirmada, cobro, compra aprobada).")
    public List<RegistroResponse> traza(@PathVariable String correlationId) {
        return service.traza(correlationId);
    }
}
