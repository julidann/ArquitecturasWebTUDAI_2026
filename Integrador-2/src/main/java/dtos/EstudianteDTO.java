package dtos;

public class EstudianteDTO {
    private String nombres;
    private String apellido;
    private int edad;
    private String genero;
    private int dni;
    private String ciudadResidencia;
    private Long lu;

    public EstudianteDTO() {}

    public EstudianteDTO(String nombres, String apellido, int edad, String genero,
                          int dni, String ciudadResidencia, Long lu) {
        this.nombres = nombres;
        this.apellido = apellido;
        this.edad = edad;
        this.genero = genero;
        this.dni = dni;
        this.ciudadResidencia = ciudadResidencia;
        this.lu = lu;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellido() {
        return apellido;
    }

    public int getEdad() {
        return edad;
    }

    public String getGenero() {
        return genero;
    }

    public int getDni() {
        return dni;
    }

    public String getCiudadResidencia() {
        return ciudadResidencia;
    }

    public Long getLu() {
        return lu;
    }

    @Override
    public String toString() {
        return "Nombres: " + nombres
                + " | Apellido: " + apellido
                + " | Edad: " + edad
                + " | Genero: " + genero
                + " | DNI: " + dni
                + " | Ciudad: " + ciudadResidencia
                + " | LU: " + lu;
    }
}
