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
    private final com.ticketya.compras.repository.EntradaRepository entradaRepository;

    public InternoController(CancelacionEventoService cancelacionService,
                             com.ticketya.compras.repository.EntradaRepository entradaRepository) {
        this.cancelacionService = cancelacionService;
        this.entradaRepository = entradaRepository;
    }

    @GetMapping("/{eventoId}/compradores")
    @Operation(summary = "[Interno] Compradores de un evento (lo usa ms-notificaciones)",
            description = "Ids de los usuarios con entradas válidas, utilizadas o canceladas del evento.")
    public java.util.List<Long> compradores(@PathVariable Long eventoId) {
        return entradaRepository.compradoresDeEvento(eventoId);
    }

    @PostMapping("/{eventoId}/cancelacion")
    @Operation(summary = "[Interno] Procesar la cancelación de un evento (lo invoca ms-eventos)",
            description = "Pasa todas las entradas válidas a CANCELADA y pide el reintegro de cada una. Idempotente.")
    public CancelacionEventoResponse cancelar(@PathVariable Long eventoId,
                                              @RequestBody(required = false) CancelacionEventoRequest body) {
        return cancelacionService.cancelarEvento(eventoId, body == null ? null : body.motivo());
    }
}
