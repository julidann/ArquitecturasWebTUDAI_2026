package com.ticketya.recomendaciones.controller;

import com.ticketya.recomendaciones.service.PreferenciasService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints para otros microservicios (el gateway no los expone). */
@Hidden
@RestController
@RequestMapping("/internal")
public class InternoController {

    private final PreferenciasService service;

    public InternoController(PreferenciasService service) {
        this.service = service;
    }

    /** Clientes que tienen alguno de esos artistas como favorito y viven en esa ciudad. */
    @GetMapping("/preferencias/interesados")
    public List<Long> interesados(@RequestParam List<String> artistas, @RequestParam String ciudad) {
        return service.interesados(artistas, ciudad);
    }
}
