package com.ticketya.eventos.controller;

import com.ticketya.eventos.service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints que solo usan otros microservicios. El gateway no los expone (ver la
 * configuracion de rutas en api-gateway).
 */
@RestController
@RequestMapping("/internal/eventos")
@Tag(name = "Interno", description = "Endpoints entre microservicios (no expuestos por el gateway)")
public class InternoController {

    private final EventoService eventoService;

    public InternoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @PostMapping("/{id}/agotado")
    @Operation(summary = "Marcar evento como AGOTADO (lo invoca ms-reservas)")
    public ResponseEntity<Void> agotado(@PathVariable Long id) {
        eventoService.marcarAgotado(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/disponible")
    @Operation(summary = "Volver un evento AGOTADO a PUBLICADO cuando se liberan lugares (lo invoca ms-reservas)")
    public ResponseEntity<Void> disponible(@PathVariable Long id) {
        eventoService.marcarDisponible(id);
        return ResponseEntity.noContent().build();
    }
}
