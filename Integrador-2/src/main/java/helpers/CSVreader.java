package helpers;

import entities.Carrera;
import entities.Estudiante;
import entities.Inscripcion;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CSVreader {

    public List<Carrera> leerArchivoCarreras() throws IOException {
        List<Carrera> carreras = new ArrayList<>();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("csv_files/carreras.csv")) {
            if (in == null) {
                throw new FileNotFoundException("No se encontró csv_files/carreras.csv en resources");
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                 CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
                for (CSVRecord row : parser) {
                    carreras.add(new Carrera(
                            Integer.parseInt(row.get("id_carrera")),
                            row.get("carrera"),
                            Integer.parseInt(row.get("duracion"))));
                }
            }
        }
        return carreras;
    }

    public List<Estudiante> leerArchivoEstudiantes() throws IOException {
        List<Estudiante> estudiantes = new ArrayList<>();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("csv_files/estudiantes.csv")) {
            if (in == null) {
                throw new FileNotFoundException("No se encontró csv_files/estudiantes.csv en resources");
            }
            // Agregado StandardCharsets.UTF_8 para evitar caracteres corruptos en nombres/ciudades
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                 CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
                for (CSVRecord row : parser) {
                    estudiantes.add(new Estudiante(
                            Integer.parseInt(row.get("DNI")),
                            row.get("nombre"),
                            row.get("apellido"),
                            Integer.parseInt(row.get("edad")),
                            row.get("genero"),
                            row.get("ciudad"),
                            Long.parseLong(row.get("LU"))));
                }
            }
        }
        return estudiantes;
    }

    public List<Inscripcion> leerArchivoEstudianteCarrera(
            List<Carrera> carreras, List<Estudiante> estudiantes) throws IOException {

        List<Inscripcion> inscripciones = new ArrayList<>();

        try (InputStream in = getClass().getClassLoader().getResourceAsStream("csv_files/estudianteCarrera.csv")) {
            if (in == null) {
                throw new FileNotFoundException("No se encontró csv_files/estudianteCarrera.csv en resources");
            }

            // Agregado StandardCharsets.UTF_8
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                 CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {

                for (CSVRecord row : parser) {
                    int dni = Integer.parseInt(row.get("id_estudiante"));
                    int idCarrera = Integer.parseInt(row.get("id_carrera"));
                    int anioInscripcion = Integer.parseInt(row.get("inscripcion"));
                    int anioGraduacion = Integer.parseInt(row.get("graduacion"));
                    int antiguedad = Integer.parseInt(row.get("antiguedad"));

                    Estudiante estudiante = estudiantes.stream()
                            .filter(e -> e.getDni() == dni)
                            .findFirst().orElse(null);

                    Carrera carrera = carreras.stream()
                            .filter(c -> c.getId() == idCarrera)
                            .findFirst().orElse(null);

                    if (estudiante == null || carrera == null) {
                        continue;
                    }

                    LocalDate fechaInscripcion = LocalDate.of(anioInscripcion, 1, 1);
                    LocalDate fechaEgreso = (anioGraduacion == 0)
                            ? null
                            : LocalDate.of(anioGraduacion, 1, 1);

                    boolean graduado = (anioGraduacion != 0);

                    inscripciones.add(new Inscripcion(
                            antiguedad, fechaInscripcion, fechaEgreso,
                            graduado, carrera, estudiante));
                }
            }
        }
        return inscripciones;
    }
}