package com.tudai.integrador3.services;

import com.tudai.integrador3.dtos.CarreraConCantInscriptosDTO;
import com.tudai.integrador3.dtos.ReporteCarreraDTO;
import com.tudai.integrador3.dtos.ResumenAnualDTO;
import com.tudai.integrador3.repositories.CarreraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarreraService {

    private final CarreraRepository carreraRepository;

    // 2f) Recuperar las carreras con estudiantes inscriptos, ordenadas por cantidad de inscriptos
    @Transactional(readOnly = true)
    public List<CarreraConCantInscriptosDTO> findCarrerasConInscriptos() {
        return carreraRepository.findCarrerasConInscriptosOrdenadas();
    }

    // 2h) Generar un reporte de las carreras (inscriptos y egresados por año)
    @Transactional(readOnly = true)
    public List<ReporteCarreraDTO> generarReporte() {
        List<Object[]> rawResults = carreraRepository.findReporteRaw();

        // Estructura auxiliar para agrupar por carrera respetando el orden alfabético
        Map<String, List<ResumenAnualDTO>> mapaReporte = new LinkedHashMap<>();

        for (Object[] row : rawResults) {
            String nombreCarrera = (String) row[0];
            int anio = ((Number) row[1]).intValue();
            Long inscriptos = ((Number) row[2]).longValue();
            Long egresados = ((Number) row[3]).longValue();

            ResumenAnualDTO resumen = new ResumenAnualDTO(anio, inscriptos, egresados);

            mapaReporte.computeIfAbsent(nombreCarrera, k -> new ArrayList<>()).add(resumen);
        }

        return mapaReporte.entrySet().stream()
                .map(entry -> new ReporteCarreraDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}