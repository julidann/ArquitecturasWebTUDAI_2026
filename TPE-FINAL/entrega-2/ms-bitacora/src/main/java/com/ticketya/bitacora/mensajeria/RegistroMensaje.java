package com.ticketya.bitacora.mensajeria;

import java.time.LocalDateTime;
import java.util.Map;

/** Lo que publica la clase Bitacora de cada servicio. */
public record RegistroMensaje(String correlationId, String servicio, String operacion, String resultado,
                              Long usuarioId, Map<String, Object> detalle, LocalDateTime fecha) {
}
