package dtos;

import entities.Inscripcion;

import java.util.*;
import java.util.stream.Collectors;

public class CarreraDTO {
    private String nombre;
    private List<Inscripcion> inscripciones = new ArrayList<>();

    public CarreraDTO() {}

    public CarreraDTO(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public List<Inscripcion> getInscripciones() {
        return new ArrayList<>(inscripciones);
    }

    public void addInscripcion(Inscripcion inscripcion) {
        inscripciones.add(inscripcion);
    }

    public List<ResumenAnualDTO> getResumenPorAnio() {

        Map<Integer, List<Inscripcion>> porAnio = new TreeMap<>();

        for (Inscripcion inscripcion : inscripciones) {

            if (inscripcion.getAnioInscripcion() != null) {

                int anio = inscripcion.getAnioInscripcion().getYear();

                if (!porAnio.containsKey(anio)) {
                    porAnio.put(anio, new ArrayList<>());
                }

                porAnio.get(anio).add(inscripcion);
            }
        }

        List<ResumenAnualDTO> resultado = new ArrayList<>();

        for (Integer anio : porAnio.keySet()) {

            List<Inscripcion> inscripcionesDelAnio = porAnio.get(anio);

            int cantidadInscriptos = inscripcionesDelAnio.size();
            int cantidadGraduados = 0;

            for (Inscripcion inscripcion : inscripcionesDelAnio) {

                if (inscripcion.isGraduado()) {
                    cantidadGraduados++;
                }
            }

            resultado.add(new ResumenAnualDTO(
                    anio,
                    cantidadInscriptos,
                    cantidadGraduados
            ));
        }

        return resultado;
    }

    @Override
    public String toString() {
        return "Carrera: " + nombre
                + " | Resumen: " + getResumenPorAnio();
    }
}
