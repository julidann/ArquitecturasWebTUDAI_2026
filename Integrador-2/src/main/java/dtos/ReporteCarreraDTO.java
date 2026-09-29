package dtos;

public class ReporteCarreraDTO {
    private final String carrera;
    private final int anio;
    private final long inscriptos;
    private final long egresados;

    public ReporteCarreraDTO(String carrera, int anio, long inscriptos, long egresados) {
        this.carrera = carrera;
        this.anio = anio;
        this.inscriptos = inscriptos;
        this.egresados = egresados;
    }

    public String getCarrera() {
        return carrera;
    }

    public int getAnio() {
        return anio;
    }

    public long getInscriptos() {
        return inscriptos;
    }

    public long getEgresados() {
        return egresados;
    }

    @Override
    public String toString() {
        return carrera + " | "
                + anio
                + " | inscriptos: " + inscriptos
                + " | egresados: "  + egresados;
    }
}