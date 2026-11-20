package com.ticketya.compras.client;

import java.time.LocalDateTime;

/** Lo que usa ms-compras del detalle de un evento de ms-eventos. */
public record EventoDto(Long id, String nombre, Long organizadorId, String estado, LocalDateTime fechaHora) {
}
