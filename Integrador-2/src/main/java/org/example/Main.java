package org.example;

import dtos.*;
import factories.JpaMySqlRepositoryFactory;
import factories.RepositoryFactory;
import helpers.CSVreader;
import helpers.DatabaseLoader;
import jakarta.persistence.EntityManager;
import repositories.interfaces.RepositoryCarrera;
import repositories.interfaces.RepositoryEstudiante;
import repositories.interfaces.RepositoryInscripcion;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {

        CSVreader reader = new CSVreader();
        RepositoryFactory factory = RepositoryFactory.getDAOFactory(RepositoryFactory.MYSQL_JDBC);

        try {
            DatabaseLoader.cargarDatos(reader, factory);

            EntityManager em = JpaMySqlRepositoryFactory
                    .getEntityManagerFactory()
                    .createEntityManager();

            RepositoryEstudiante estudiantes = factory.getEstudianteRepository(em);
            RepositoryCarrera carreras = factory.getCarreraRepository(em);
            RepositoryInscripcion inscripciones = factory.getInscripcionRepository(em);

            // --- 2a) DAR DE ALTA UN ESTUDIANTE ---
            /*
            Estudiante nuevoEstudiante = new Estudiante(
                    39550725,
                    "Julieta",
                    "D'Annunzio",
                    24,
                    "Female",
                    "Tandil",
                    40000L
            );
            estudiantes.save(nuevoEstudiante);
            */

            // --- 2b) MATRICULAR UN ESTUDIANTE EN UNA CARRERA ---
            /*
            carreras.matricularEstudianteEnCarrera(34978L, "TUDAI");
            */

            System.out.println("\n---2c) MOSTRAR TODOS LOS ESTUDIANTES ORDENADOS POR NOMBRE---");

            for (EstudianteDTO estudiante : estudiantes.obtenerEstudiantesOrdenadosPorNombre()) {
                System.out.println(estudiante);
            }

            System.out.println("\n--- 2d) BUSCAR ESTUDIANTE POR NRO DE LIBRETA UNIVERSITARIA ---");
            System.out.println(estudiantes.buscarPorLibreta(34978L));

            System.out.println("\n--- 2e) MOSTRAR TODOS LOS ESTUDIANTES DE GÉNERO FEMENINO ===");

            for (EstudianteDTO estudiante : estudiantes.buscarPorGenero("Female")) {
                System.out.println(estudiante);
            }

            System.out.println("\n--- 2f) MOSTRAR CARRERAS CON ESTUDIANTES INSCRIPTOS,ORDENADOS POR CANTIDAD DE INSCRIPTOS ---");

            for (CarreraConCantInscriptosDTO carrera : inscripciones.recuperarCarrerasOrdenadasPorCantidadInscriptos()) {
                System.out.println(carrera);
            }

            System.out.println("\n--- 2g) MOSTRAR ESTUDIANTES POR CARRERA Y CIUDAD ---");

            for (InscripcionDTO inscripcion : inscripciones.estudiantesDeCarreraPorCiudad("TUDAI", "Rauch")) {
                System.out.println(inscripcion);
            }

            System.out.println("\n--- 3) REPORTE DE CARRERAS ---");

            for (ReporteCarreraDTO reporte : carreras.reporteCarreras()) {
                System.out.println(reporte);
            }

            em.close();
            JpaMySqlRepositoryFactory.getEntityManagerFactory().close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}