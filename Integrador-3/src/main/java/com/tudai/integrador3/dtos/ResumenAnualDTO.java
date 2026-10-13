package com.tudai.integrador3.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenAnualDTO {
    private int anio;
    private Long cantidadInscriptos;
    private Long cantidadEgresados;
}
