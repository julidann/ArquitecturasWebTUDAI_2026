package com.ticketya.fila.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record TurnoResponse(
        @Schema(example = "7") Long eventoId,
        @Schema(example = "4") Long usuarioId,
        @Schema(example = "EN_ESPERA") EstadoTurno estado,
        @Schema(description = "Posición en la fila (1 = el próximo). Null si ya está habilitado.", example = "350") Long posicion,
        @Schema(description = "Clientes esperando en total", example = "12000") Long enEspera,
        @Schema(description = "Estimación en segundos hasta que lo habiliten", example = "40") Long esperaEstimadaSegundos,
        @Schema(description = "Token de turno: se envía en el header X-Turno-Fila al reservar. Solo si está HABILITADO.")
        String turno,
        @Schema(description = "Hasta cuándo puede empezar su compra. Solo si está HABILITADO.") Instant habilitadoHasta) {
}
