package com.ticketya.recomendaciones.controller;

import com.ticketya.recomendaciones.dto.PreferenciasRequest;
import com.ticketya.recomendaciones.dto.PreferenciasResponse;
import com.ticketya.recomendaciones.service.PreferenciasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/preferencias")
@Tag(name = "Preferencias", description = "Preferencias del cliente logueado (rol CLIENTE)")
public class PreferenciasController {

    private final PreferenciasService service;

    public PreferenciasController(PreferenciasService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Ver mis preferencias", description = "Si nunca las cargó devuelve las listas vacías.")
    public PreferenciasResponse obtener(@Parameter(hidden = true) @RequestHeader("X-Usuario-Id") long usuarioId) {
        return service.obtener(usuarioId);
    }

    @PutMapping
    @Operation(summary = "Guardar mis preferencias",
            description = "Reemplaza las preferencias completas: tipos de evento, artistas favoritos y ciudad.")
    public PreferenciasResponse guardar(@Valid @RequestBody PreferenciasRequest request,
                                        @Parameter(hidden = true) @RequestHeader("X-Usuario-Id") long usuarioId) {
        return service.guardar(usuarioId, request);
    }
}
