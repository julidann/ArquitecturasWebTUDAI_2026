package com.ticketya.recomendaciones.dto;

import com.ticketya.recomendaciones.model.EventoCatalogo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Evento sugerido, con el puntaje y los motivos (para que el cliente entienda por que se lo sugerimos). */
public record RecomendacionResponse(Long eventoId, String nombre, String tipo, List<String> participantes,
                                    String lugar, String ciudad, LocalDateTime fechaHora, BigDecimal precioMinimo,
                                    int puntaje, List<String> motivos) {

    public static RecomendacionResponse de(EventoCatalogo e, int puntaje, List<String> motivos) {
        return new RecomendacionResponse(e.getEventoId(), e.getNombre(), e.getTipo(), e.getParticipantes(),
                e.getLugar(), e.getCiudad(), e.getFechaHora(), e.getPrecioMinimo(), puntaje, motivos);
    }
}
