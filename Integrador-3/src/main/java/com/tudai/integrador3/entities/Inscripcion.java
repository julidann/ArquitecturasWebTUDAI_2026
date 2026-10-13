package com.tudai.integrador3.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private int antiguedad;
    private LocalDate anioInscripcion;
    private LocalDate anioEgreso;
    private boolean graduado;

    @ManyToOne
    @JoinColumn(name = "id_carrera", nullable = false)
    private Carrera carrera;

    @ManyToOne
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    public Inscripcion(Carrera carrera, Estudiante estudiante) {
        this.carrera = carrera;
        this.estudiante = estudiante;
        this.anioInscripcion = LocalDate.now();
        this.antiguedad = 0;
        this.graduado = false;
    }
    //Este constructor es para el ID del CSVreader
    public Inscripcion(int antiguedad, LocalDate anioInscripcion, LocalDate anioEgreso,
                       boolean graduado, Carrera carrera, Estudiante estudiante) {
        this.antiguedad = antiguedad;
        this.anioInscripcion = anioInscripcion;
        this.anioEgreso = anioEgreso;
        this.graduado = graduado;
        this.carrera = carrera;
        this.estudiante = estudiante;
    }
}