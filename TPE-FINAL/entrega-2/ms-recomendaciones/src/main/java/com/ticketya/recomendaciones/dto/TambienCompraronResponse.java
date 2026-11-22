package com.ticketya.recomendaciones.dto;

import java.time.LocalDateTime;

/** "Quienes compraron entradas para este evento también compraron para..." */
public record TambienCompraronResponse(Long eventoId, String nombre, String tipo, String ciudad,
                                       LocalDateTime fechaHora, long compradoresEnComun) {
}
