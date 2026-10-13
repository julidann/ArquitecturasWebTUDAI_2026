package com.tudai.integrador3.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstudianteDTO {
    private int dni;
    private String nombres;
    private String apellido;
    private int edad;
    private String genero;
    private String ciudadResidencia;
    private Long lu;
}
