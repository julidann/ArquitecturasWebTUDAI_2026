package com.ticketya.recomendaciones.dto;

import com.ticketya.recomendaciones.model.Preferencias;

import java.time.LocalDateTime;
import java.util.List;

public record PreferenciasResponse(Long usuarioId, List<String> tiposEvento, List<String> artistasFavoritos,
                                   String ciudad, LocalDateTime actualizadas) {

    public static PreferenciasResponse de(Preferencias p) {
        return new PreferenciasResponse(p.getUsuarioId(), p.getTiposEvento(), p.getArtistasFavoritos(), p.getCiudad(),
                p.getActualizadas());
    }

    public static PreferenciasResponse vacias(Long usuarioId) {
        return new PreferenciasResponse(usuarioId, List.of(), List.of(), null, null);
    }
}
