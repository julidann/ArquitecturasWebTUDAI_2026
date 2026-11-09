package com.ticketya.compras.controller;

import com.ticketya.compras.dto.CompraRequest;
import com.ticketya.compras.dto.CompraResponse;
import com.ticketya.compras.exception.ErrorResponse;
import com.ticketya.compras.service.CompraService;
import com.ticketya.compras.service.ResultadoCompra;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/compras")
@Tag(name = "Compras", description = "Pago de una reserva y emisión de entradas")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @PostMapping
    @Operation(summary = "Pagar una reserva",
            description = """
                    Cobra la reserva y, si el pago se aprueba, emite una entrada con código QR por cada lugar.
                    Hay que enviar una **Idempotency-Key** única por intento de compra (ej: un UUID generado por la app).
                    Si se reenvía la misma clave se devuelve el resultado original (200) sin volver a cobrar; si la
                    compra había quedado en ERROR, se reintenta.""")
    @ApiResponse(responseCode = "201", description = "Pago aprobado, entradas emitidas")
    @ApiResponse(responseCode = "200", description = "Reintento con la misma Idempotency-Key: resultado original")
    @ApiResponse(responseCode = "402", description = "Pago rechazado; la reserva se liberó (el cuerpo es la compra RECHAZADA)",
            content = @Content(schema = @Schema(implementation = CompraResponse.class)))
    @ApiResponse(responseCode = "403", description = "La reserva es de otro usuario",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No existe la reserva",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Reserva vencida/liberada, compra en proceso o ya existe una compra para la reserva",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "La Idempotency-Key ya se usó para otra compra",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Falló otro servicio; la compra queda en ERROR y se puede reintentar con la misma clave",
            content = @Content(schema = @Schema(implementation = CompraResponse.class)))
    public ResponseEntity<CompraResponse> comprar(
            @Valid @RequestBody CompraRequest request,
            @Parameter(example = "100") @RequestHeader("X-Usuario-Id") Long usuarioId,
            @Parameter(example = "3f6c1b8e-2a41-4d0c-9b7e-5e2f0a1c9d33") @RequestHeader("Idempotency-Key") String clave) {
        ResultadoCompra r = compraService.comprar(usuarioId, request, clave);
        return ResponseEntity.status(r.status()).body(r.compra());
    }

    @GetMapping
    @Operation(summary = "Mis compras")
    public List<CompraResponse> mias(@RequestHeader("X-Usuario-Id") Long usuarioId) {
        return compraService.deUsuario(usuarioId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una compra (con sus entradas)")
    @ApiResponse(responseCode = "403", description = "La compra es de otro usuario",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public CompraResponse obtener(@PathVariable Long id,
                                  @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        return compraService.obtener(id, usuarioId);
    }
}
