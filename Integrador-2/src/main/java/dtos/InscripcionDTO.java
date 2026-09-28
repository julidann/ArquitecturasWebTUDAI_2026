package dtos;

import java.time.LocalDate;

public class InscripcionDTO {
    private int antiguedad;
    private LocalDate anioInscripcion;
    private LocalDate anioEgreso;
    private boolean graduado;
    private String nombreCarrera;
    private Long luEstudiante;
    private EstudianteDTO estudiante;

    public InscripcionDTO() {}

    public InscripcionDTO(int antiguedad, LocalDate anioInscripcion, LocalDate anioEgreso,
                          boolean graduado, String nombreCarrera, Long luEstudiante) {
        this.antiguedad = antiguedad;
        this.anioInscripcion = anioInscripcion;
        this.anioEgreso = anioEgreso;
        this.graduado = graduado;
        this.nombreCarrera = nombreCarrera;
        this.luEstudiante = luEstudiante;
    }

    public int getAntiguedad() {
        return antiguedad;
    }

    public void setAntiguedad(int antiguedad) {
        this.antiguedad = antiguedad;
    }

    public LocalDate getAnioInscripcion() {
        return anioInscripcion;
    }

    public void setAnioInscripcion(LocalDate anioInscripcion) {
        this.anioInscripcion = anioInscripcion;
    }

    public LocalDate getAnioEgreso() {
        return anioEgreso;
    }

    public void setAnioEgreso(LocalDate anioEgreso) {
        this.anioEgreso = anioEgreso;
    }

    public boolean isGraduado() {
        return graduado;
    }

    public void setGraduado(boolean graduado) {
        this.graduado = graduado;
    }

    public String getNombreCarrera() {
        return nombreCarrera;
    }

    public void setNombreCarrera(String nombreCarrera) {
        this.nombreCarrera = nombreCarrera;
    }

    public Long getLuEstudiante() {
        return luEstudiante;
    }

    public void setLuEstudiante(Long luEstudiante) {
        this.luEstudiante = luEstudiante;
    }

    public EstudianteDTO getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(EstudianteDTO estudiante) {
        this.estudiante = estudiante;
    }

    @Override
    public String toString() {
        return "Antiguedad: " + antiguedad
                + " | Año inscripcion: " + anioInscripcion
                + " | Año egreso: " + anioEgreso
                + " | Graduado: " + graduado
                + " | Carrera: " + nombreCarrera
                + " | LU estudiante: " + luEstudiante;
    }
}
