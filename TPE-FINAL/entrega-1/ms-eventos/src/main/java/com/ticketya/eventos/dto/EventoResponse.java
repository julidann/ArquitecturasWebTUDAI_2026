package com.ticketya.eventos.dto;

import com.ticketya.eventos.model.EstadoEvento;
import com.ticketya.eventos.model.Evento;
import com.ticketya.eventos.model.TipoEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record EventoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Los Piojos - Ritual 2026") String nombre,
        @Schema(example = "Show de regreso en el Estadio Único") String descripcion,
        @Schema(example = "RECITAL") TipoEvento tipo,
        @Schema(example = "10") Long organizadorId,
        List<String> participantes,
        @Schema(example = "2026-12-15T21:00:00") LocalDateTime fechaHora,
        LugarResumen lugar,
        @Schema(example = "PUBLICADO") EstadoEvento estado,
        boolean altaDemanda,
        @Schema(example = "4") Integer limiteEntradasPorCliente,
        @Schema(example = "25000.00") BigDecimal precioMinimo,
        List<PrecioSectorResponse> precios) {

    public record LugarResumen(
            @Schema(example = "1") Long id,
            @Schema(example = "Estadio Único") String nombre,
            @Schema(example = "La Plata") String ciudad) {
    }

    public static EventoResponse de(Evento e) {
        return new EventoResponse(e.getId(), e.getNombre(), e.getDescripcion(), e.getTipo(), e.getOrganizadorId(),
                List.copyOf(e.getParticipantes()), e.getFechaHora(),
                new LugarResumen(e.getLugar().getId(), e.getLugar().getNombre(), e.getLugar().getCiudad()),
                e.getEstado(), e.isAltaDemanda(), e.getLimiteEntradasPorCliente(), e.getPrecioMinimo(),
                e.getPrecios().stream()
                        .sorted(Comparator.comparing(p -> p.getSector().getId()))
                        .map(PrecioSectorResponse::de)
                        .toList());
    }
}
