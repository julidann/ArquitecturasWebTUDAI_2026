package com.tudai.integrador3.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteCarreraDTO {
    private String nombreCarrera;
    private List<ResumenAnualDTO> años;
}
