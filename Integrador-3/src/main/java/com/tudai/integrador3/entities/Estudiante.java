package com.tudai.integrador3.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Estudiante {

    @Id
    private int dni;

    @Column(nullable = false)
    private String nombres;

    @Column(nullable = false)
    private String apellido;

    private int edad;
    private String genero;
    private String ciudadResidencia;

    @Column(unique = true)
    private Long lu;

    @OneToMany(mappedBy = "estudiante", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Inscripcion> inscripciones = new ArrayList<>();

    public Estudiante(int dni, String nombres, String apellido, int edad, String genero, String ciudadResidencia, Long lu) {
        this.dni = dni;
        this.nombres = nombres;
        this.apellido = apellido;
        this.edad = edad;
        this.genero = genero;
        this.ciudadResidencia = ciudadResidencia;
        this.lu = lu;
    }

    public void addInscripcion(Inscripcion inscripcion) {
        if (!inscripciones.contains(inscripcion)) {
            inscripciones.add(inscripcion);
            inscripcion.setEstudiante(this);
        }
    }

    public void removeInscripcion(Inscripcion inscripcion) {
        if (inscripciones.remove(inscripcion)) {
            inscripcion.setEstudiante(null);
        }
    }
}