package dtos;

public class CarreraConCantInscriptosDTO {
    private String nombre;
    private long cantInscriptos;

    public CarreraConCantInscriptosDTO() {}

    public CarreraConCantInscriptosDTO(String nombre, long cantInscriptos) {
        this.nombre = nombre;
        this.cantInscriptos = cantInscriptos;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public long getCantInscriptos() {
        return cantInscriptos;
    }

    public void setCantInscriptos(long cantInscriptos) {
        this.cantInscriptos = cantInscriptos;
    }

    @Override
    public String toString() {
        return "Carrera: " + nombre
                + " | Inscriptos: " + cantInscriptos;
    }
}
