package com.tudai.integrador3.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarreraConCantInscriptosDTO {
    private int idCarrera;
    private String nombreCarrera;
    private Long cantidadInscriptos;
}
