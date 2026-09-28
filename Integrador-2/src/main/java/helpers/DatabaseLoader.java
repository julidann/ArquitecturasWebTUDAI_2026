package helpers;

import entities.Carrera;
import entities.Estudiante;
import entities.Inscripcion;
import factories.RepositoryFactory;
import jakarta.persistence.EntityManager;
import factories.JpaMySqlRepositoryFactory;
import repositories.interfaces.RepositoryCarrera;
import repositories.interfaces.RepositoryEstudiante;
import repositories.interfaces.RepositoryInscripcion;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DatabaseLoader {

    public static void cargarDatos(CSVreader reader, RepositoryFactory factory)
            throws SQLException, IOException {

        EntityManager em = JpaMySqlRepositoryFactory
                .getEntityManagerFactory()
                .createEntityManager();

        List<Carrera> carreras = reader.leerArchivoCarreras();
        List<Estudiante> estudiantes = reader.leerArchivoEstudiantes();

        RepositoryCarrera carreraRepository = factory.getCarreraRepository(em);
        RepositoryEstudiante estudianteRepository = factory.getEstudianteRepository(em);
        RepositoryInscripcion inscripcionRepository = factory.getInscripcionRepository(em);

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

        em.close();
    }
}