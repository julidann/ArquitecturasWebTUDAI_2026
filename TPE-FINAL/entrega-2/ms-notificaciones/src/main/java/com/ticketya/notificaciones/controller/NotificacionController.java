package com.ticketya.notificaciones.controller;

import com.ticketya.notificaciones.dto.NotificacionResponse;
import com.ticketya.notificaciones.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notificaciones")
@Validated
@Tag(name = "Notificaciones")
public class NotificacionController {

    private final NotificacionService service;

    public NotificacionController(NotificacionService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Mis notificaciones (cualquier usuario logueado)",
            description = "Bandeja de notificaciones del usuario, de la más nueva a la más vieja, con su estado de envío.")
    public List<NotificacionResponse> mias(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limite,
                                           @Parameter(hidden = true) @RequestHeader("X-Usuario-Id") long usuarioId) {
        return service.deUsuario(usuarioId, limite);
    }
}
