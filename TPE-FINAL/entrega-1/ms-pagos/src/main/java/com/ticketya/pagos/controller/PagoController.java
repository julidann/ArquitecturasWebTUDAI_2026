package com.ticketya.pagos.controller;

import com.ticketya.pagos.dto.*;
import com.ticketya.pagos.exception.ErrorResponse;
import com.ticketya.pagos.service.PagoService;
import com.ticketya.pagos.service.PagoService.ResultadoOperacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/pagos")
@Tag(name = "Pagos", description = "Pasarela de pagos simulada (mock)")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    @Operation(summary = "Cobrar",
            description = "Registra un intento de cobro. La respuesta indica si fue APROBADO o RECHAZADO (los dos casos "
                    + "son 201 porque el intento queda registrado). Reenviar la misma Idempotency-Key devuelve el pago "
                    + "original con 200 y no se vuelve a cobrar.")
    @ApiResponse(responseCode = "201", description = "Pago procesado (aprobado o rechazado)")
    @ApiResponse(responseCode = "200", description = "Pedido repetido: se devuelve el pago original")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o falta Idempotency-Key",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "La Idempotency-Key ya se usó con otros datos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<PagoResponse> pagar(@Valid @RequestBody PagoRequest request,
                                              @Parameter(example = "compra-21") @RequestHeader("Idempotency-Key") String clave) {
        return responder(pagoService.pagar(request, clave));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un pago con sus reintegros")
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PagoResponse obtener(@PathVariable Long id) {
        return pagoService.obtener(id);
    }

    @GetMapping
    @Operation(summary = "Pagos de una compra")
    public List<PagoResponse> deCompra(@RequestParam Long compraId) {
        return pagoService.deCompra(compraId);
    }

    @PostMapping("/{id}/reintegros")
    @Operation(summary = "Reintegrar (total o parcial)",
            description = "Devuelve dinero de un pago aprobado. La suma de reintegros no puede superar el monto cobrado. "
                    + "Idempotente por Idempotency-Key.")
    @ApiResponse(responseCode = "201", description = "Reintegro realizado")
    @ApiResponse(responseCode = "200", description = "Pedido repetido: se devuelve el reintegro original")
    @ApiResponse(responseCode = "409", description = "El pago no está aprobado o el monto supera lo cobrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ReintegroResponse> reintegrar(@PathVariable Long id, @Valid @RequestBody ReintegroRequest request,
                                                        @Parameter(example = "devolucion-5") @RequestHeader("Idempotency-Key") String clave) {
        return responder(pagoService.reintegrar(id, request, clave));
    }

    private <T> ResponseEntity<T> responder(ResultadoOperacion<T> r) {
        return ResponseEntity.status(r.creado() ? HttpStatus.CREATED : HttpStatus.OK).body(r.cuerpo());
    }
}
