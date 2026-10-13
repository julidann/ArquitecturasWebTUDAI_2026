package com.tudai.integrador3.helpers;

import com.tudai.integrador3.entities.Carrera;
import com.tudai.integrador3.entities.Estudiante;
import com.tudai.integrador3.entities.Inscripcion;
import com.tudai.integrador3.repositories.CarreraRepository;
import com.tudai.integrador3.repositories.EstudianteRepository;
import com.tudai.integrador3.repositories.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseLoader implements CommandLineRunner {

    private final CSVreader csVreader;
    private final CarreraRepository carreraRepository;
    private final EstudianteRepository estudianteRepository;
    private final InscripcionRepository inscripcionRepository;

    @Override
    public void run(String... args) throws Exception {
        if (carreraRepository.count() == 0) {
            cargarDatos(csVreader);
        }
    }

    public void cargarDatos(CSVreader reader) throws SQLException, IOException {

        List<Carrera> carreras = reader.leerArchivoCarreras();
        List<Estudiante> estudiantes = reader.leerArchivoEstudiantes();

        for (Carrera carrera : carreras) {
            carreraRepository.save(carrera);
        }

        for (Estudiante estudiante : estudiantes) {
            estudianteRepository.save(estudiante);
        }

        List<Inscripcion> inscripciones =
                reader.leerArchivoEstudianteCarrera(carreras, estudiantes);

        for (Inscripcion inscripcion : inscripciones) {
            inscripcionRepository.save(inscripcion);
        }
    }
}