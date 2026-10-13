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
public class Carrera {

    @Id
    private int id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private int duracion;

    @OneToMany(mappedBy = "carrera", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Inscripcion> inscripciones = new ArrayList<>();

    public Carrera(int id, String nombre, int duracion) {
        this.id = id;
        this.nombre = nombre;
        this.duracion = duracion;
    }

    public void addInscripcion(Inscripcion inscripcion) {
        if (!inscripciones.contains(inscripcion)) {
            inscripciones.add(inscripcion);
            inscripcion.setCarrera(this);
        }
    }

    public void removeInscripcion(Inscripcion inscripcion) {
        if (inscripciones.remove(inscripcion)) {
            inscripcion.setCarrera(null);
        }
    }
}