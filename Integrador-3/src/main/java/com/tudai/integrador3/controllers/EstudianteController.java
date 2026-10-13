package com.tudai.integrador3.controllers;

import com.tudai.integrador3.dtos.EstudianteDTO;
import com.tudai.integrador3.dtos.InscripcionDTO;
import com.tudai.integrador3.services.EstudianteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
public class EstudianteController {

    private final EstudianteService estudianteService;

    // 2a) Dar de alta un estudiante
    // (POST http://localhost:8080/api/estudiantes)
    @PostMapping
    public ResponseEntity<EstudianteDTO> save(@RequestBody EstudianteDTO dto) {
        EstudianteDTO nuevo = estudianteService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    // 2b) Matricular un estudiante en una carrera
    // (POST http://localhost:8080/api/estudiantes/matricular)
    @PostMapping("/matricular")
    public ResponseEntity<String> matricular(@RequestBody InscripcionDTO dto) {
        estudianteService.matricular(dto);
        return ResponseEntity.ok("Estudiante matriculado con éxito.");
    }

    // 2c) Recuperar todos los estudiantes ordenados por apellido
    // (GET http://localhost:8080/api/estudiantes)
    @GetMapping
    public ResponseEntity<List<EstudianteDTO>> findAllOrdered() {
        return ResponseEntity.ok(estudianteService.findAllOrdered());
    }

    // 2d) Recuperar estudiante por libreta universitaria
    // (GET http://localhost:8080/api/estudiantes/lu/12345)
    @GetMapping("/lu/{lu}")
    public ResponseEntity<EstudianteDTO> getByLu(@PathVariable Long lu) {
        return ResponseEntity.ok(estudianteService.findByLu(lu));
    }

    // 2e) Recuperar estudiantes por género
    // (GET http://localhost:8080/api/estudiantes/genero/female)
    @GetMapping("/genero/{genero}")
    public ResponseEntity<List<EstudianteDTO>> getByGenero(@PathVariable String genero) {
        return ResponseEntity.ok(estudianteService.findByGenero(genero));
    }

    // 2g) Recuperar estudiantes de una carrera filtrados por ciudad
    // (GET http://localhost:8080/api/estudiantes/carrera/1/ciudad/Rauch)
    @GetMapping("/carrera/{idCarrera}/ciudad/{ciudad}")
    public ResponseEntity<List<EstudianteDTO>> getByCarreraAndCiudad(
            @PathVariable int idCarrera,
            @PathVariable String ciudad) {
        return ResponseEntity.ok(estudianteService.findByCarreraAndCiudad(idCarrera, ciudad));
    }
}
