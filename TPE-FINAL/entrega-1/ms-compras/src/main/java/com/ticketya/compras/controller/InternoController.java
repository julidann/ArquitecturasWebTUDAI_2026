package com.ticketya.compras.controller;

import com.ticketya.compras.dto.CancelacionEventoRequest;
import com.ticketya.compras.dto.CancelacionEventoResponse;
import com.ticketya.compras.service.CancelacionEventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/eventos")
@Tag(name = "Interno", description = "Endpoints entre microservicios (no expuestos por el gateway)")
public class InternoController {

    private final CancelacionEventoService cancelacionService;

    public InternoController(CancelacionEventoService cancelacionService) {
        this.cancelacionService = cancelacionService;
    }

    @PostMapping("/{eventoId}/cancelacion")
    @Operation(summary = "[Interno] Procesar la cancelación de un evento (lo invoca ms-eventos)",
            description = "Pasa todas las entradas válidas a CANCELADA y pide el reintegro de cada una. Idempotente.")
    public CancelacionEventoResponse cancelar(@PathVariable Long eventoId,
                                              @RequestBody(required = false) CancelacionEventoRequest body) {
        return cancelacionService.cancelarEvento(eventoId, body == null ? null : body.motivo());
    }
}
