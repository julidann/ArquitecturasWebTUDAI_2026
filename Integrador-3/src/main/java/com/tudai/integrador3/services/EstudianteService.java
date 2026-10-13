package com.tudai.integrador3.services;

import com.tudai.integrador3.dtos.EstudianteDTO;
import com.tudai.integrador3.dtos.InscripcionDTO;
import com.tudai.integrador3.entities.Carrera;
import com.tudai.integrador3.entities.Estudiante;
import com.tudai.integrador3.entities.Inscripcion;
import com.tudai.integrador3.repositories.CarreraRepository;
import com.tudai.integrador3.repositories.EstudianteRepository;
import com.tudai.integrador3.repositories.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final CarreraRepository carreraRepository;
    private final InscripcionRepository inscripcionRepository;

    // 2a) Dar de alta un estudiante
    @Transactional
    public EstudianteDTO save(EstudianteDTO dto) {
        Estudiante estudiante = new Estudiante(
                dto.getDni(),
                dto.getNombres(),
                dto.getApellido(),
                dto.getEdad(),
                dto.getGenero(),
                dto.getCiudadResidencia(),
                dto.getLu()
        );
        Estudiante guardado = estudianteRepository.save(estudiante);
        return convertToDTO(guardado);
    }

    // 2b) Matricular un estudiante en una carrera
    @Transactional
    public void matricular(InscripcionDTO dto) {
        Estudiante estudiante = estudianteRepository.findById(dto.getDniEstudiante())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado con DNI: " + dto.getDniEstudiante()));

        Carrera carrera = carreraRepository.findById(dto.getIdCarrera())
                .orElseThrow(() -> new RuntimeException("Carrera no encontrada con ID: " + dto.getIdCarrera()));

        Inscripcion inscripcion = new Inscripcion(carrera, estudiante);
        inscripcionRepository.save(inscripcion);
    }

    // 2c) Recuperar todos los estudiantes ordenados por apellido
    @Transactional(readOnly = true)
    public List<EstudianteDTO> findAllOrdered() {
        return estudianteRepository.findAllByOrderByApellidoAsc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 2d) Recuperar estudiante por libreta universitaria (LU)
    @Transactional(readOnly = true)
    public EstudianteDTO findByLu(Long lu) {
        Estudiante estudiante = estudianteRepository.findByLu(lu)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado con LU: " + lu));
        return convertToDTO(estudiante);
    }

    // 2e) Recuperar estudiantes por género
    @Transactional(readOnly = true)
    public List<EstudianteDTO> findByGenero(String genero) {
        return estudianteRepository.findByGeneroIgnoreCase(genero)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 2g) Recuperar estudiantes de una carrera filtrados por ciudad de residencia
    @Transactional(readOnly = true)
    public List<EstudianteDTO> findByCarreraAndCiudad(int idCarrera, String ciudad) {
        return estudianteRepository.findByCarreraAndCiudad(idCarrera, ciudad)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Método auxiliar privado para mapear Entity -> DTO
    private EstudianteDTO convertToDTO(Estudiante e) {
        return new EstudianteDTO(
                e.getDni(),
                e.getNombres(),
                e.getApellido(),
                e.getEdad(),
                e.getGenero(),
                e.getCiudadResidencia(),
                e.getLu()
        );
    }
}
