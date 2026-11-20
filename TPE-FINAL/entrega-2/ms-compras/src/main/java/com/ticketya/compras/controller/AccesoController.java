package com.ticketya.compras.controller;

import com.ticketya.compras.dto.ResumenAccesoResponse;
import com.ticketya.compras.dto.ValidacionAccesoRequest;
import com.ticketya.compras.dto.ValidacionAccesoResponse;
import com.ticketya.compras.service.AccesoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accesos")
@Tag(name = "Control de acceso", description = "Validación del QR en el ingreso al evento")
public class AccesoController {

    private final AccesoService accesoService;

    public AccesoController(AccesoService accesoService) {
        this.accesoService = accesoService;
    }

    @PostMapping("/validar")
    @Operation(summary = "Validar el QR de una entrada (ACCESO)",
            description = "Si la entrada existe, es del evento y está VALIDA se marca UTILIZADA y se deja pasar. "
                    + "Un QR no puede ingresar dos veces, aunque se escanee en dos puertas al mismo tiempo.")
    @ApiResponse(responseCode = "200", description = "PERMITIDO: puede ingresar")
    @ApiResponse(responseCode = "409", description = "Rechazado: YA_UTILIZADA, OTRO_EVENTO, DEVUELTA o CANCELADA "
            + "(el cuerpo dice el motivo)")
    @ApiResponse(responseCode = "404", description = "INEXISTENTE: el QR no corresponde a ninguna entrada")
    public ResponseEntity<ValidacionAccesoResponse> validar(
            @Valid @RequestBody ValidacionAccesoRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "X-Usuario-Id", required = false) Long personalId) {
        ValidacionAccesoResponse r = accesoService.validar(request, personalId);
        HttpStatus status = r.permitido() ? HttpStatus.OK
                : "INEXISTENTE".equals(r.resultado()) ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(r);
    }

    @GetMapping("/eventos/{eventoId}")
    @Operation(summary = "Resumen de ingresos de un evento (ACCESO, ORGANIZADOR, ADMIN)")
    public ResumenAccesoResponse resumen(@PathVariable Long eventoId) {
        return accesoService.resumen(eventoId);
    }
}
