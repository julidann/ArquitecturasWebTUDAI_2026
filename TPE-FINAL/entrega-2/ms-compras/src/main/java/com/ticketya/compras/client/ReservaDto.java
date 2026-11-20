package com.ticketya.compras.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Respuesta de ms-reservas (solo los campos que usamos). */
public record ReservaDto(
        Long id,
        Long usuarioId,
        Long eventoId,
        String nombreEvento,
        LocalDateTime fechaEvento,
        Long sectorId,
        String sector,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal montoTotal,
        String estado,
        LocalDateTime expiraEn,
        long segundosRestantes,
        List<Item> items) {

    public record Item(Long asientoId, String fila, Integer numero) {
    }

    public boolean estaPendiente() {
        return "PENDIENTE".equals(estado) && segundosRestantes > 0;
    }
}
