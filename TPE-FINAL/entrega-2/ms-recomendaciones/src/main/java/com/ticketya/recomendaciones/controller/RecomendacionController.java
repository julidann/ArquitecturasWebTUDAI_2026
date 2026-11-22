package com.ticketya.recomendaciones.controller;

import com.ticketya.recomendaciones.dto.RecomendacionResponse;
import com.ticketya.recomendaciones.dto.TambienCompraronResponse;
import com.ticketya.recomendaciones.service.RecomendacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recomendaciones")
@Validated
@Tag(name = "Recomendaciones")
public class RecomendacionController {

    private final RecomendacionService service;

    public RecomendacionController(RecomendacionService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Eventos sugeridos para mí (CLIENTE)",
            description = "Combina artistas favoritos, tipos de evento y ciudad de las preferencias con el historial "
                    + "de compras (co-compra). Cada sugerencia trae el puntaje y los motivos.")
    public List<RecomendacionResponse> paraMi(@RequestParam(defaultValue = "10") @Min(1) @Max(50) int limite,
                                              @Parameter(hidden = true) @RequestHeader("X-Usuario-Id") long usuarioId) {
        return service.recomendar(usuarioId, limite);
    }

    @GetMapping("/eventos/{eventoId}/tambien-compraron")
    @SecurityRequirements
    @Operation(summary = "Quienes compraron este evento también compraron... (público)")
    public List<TambienCompraronResponse> tambienCompraron(@PathVariable Long eventoId,
                                                           @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limite) {
        return service.tambienCompraron(eventoId, limite);
    }
}
