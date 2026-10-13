package com.tudai.integrador3.controllers;

import com.tudai.integrador3.dtos.CarreraConCantInscriptosDTO;
import com.tudai.integrador3.dtos.ReporteCarreraDTO;
import com.tudai.integrador3.services.CarreraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carreras")
@RequiredArgsConstructor
public class CarreraController {

    private final CarreraService carreraService;

    // 2f) Recuperar carreras con inscriptos ordenadas
    // (GET http://localhost:8080/api/carreras/con-inscriptos)
    @GetMapping("/con-inscriptos")
    public ResponseEntity<List<CarreraConCantInscriptosDTO>> getCarrerasConInscriptos() {
        return ResponseEntity.ok(carreraService.findCarrerasConInscriptos());
    }

    // 2h) Generar reporte de carreras
    // (GET http://localhost:8080/api/carreras/reporte)
    @GetMapping("/reporte")
    public ResponseEntity<List<ReporteCarreraDTO>> getReporte() {
        return ResponseEntity.ok(carreraService.generarReporte());
    }
}