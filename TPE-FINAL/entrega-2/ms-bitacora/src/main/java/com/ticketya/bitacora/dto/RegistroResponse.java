package com.ticketya.bitacora.dto;

import com.ticketya.bitacora.model.Registro;

import java.time.LocalDateTime;
import java.util.Map;

public record RegistroResponse(String id, String correlationId, String servicio, String operacion, String resultado,
                               Long usuarioId, Map<String, Object> detalle, LocalDateTime fecha) {

    public static RegistroResponse de(Registro r) {
        return new RegistroResponse(r.getId(), r.getCorrelationId(), r.getServicio(), r.getOperacion(),
                r.getResultado(), r.getUsuarioId(), r.getDetalle(), r.getFecha());
    }
}
