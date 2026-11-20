package com.ticketya.compras.dto;

import com.ticketya.compras.model.EstadoSolicitud;
import com.ticketya.compras.model.SolicitudDevolucion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SolicitudDevolucionResponse(
        @Schema(example = "5") Long id,
        @Schema(example = "31") Long entradaId,
        @Schema(example = "1") Long eventoId,
        @Schema(example = "100") Long usuarioId,
        @Schema(example = "45000.00") BigDecimal montoAReintegrar,
        @Schema(example = "No puedo asistir") String motivo,
        @Schema(example = "PENDIENTE") EstadoSolicitud estado,
        @Schema(example = "2026-10-25T09:00:00") LocalDateTime solicitadaEn,
        @Schema(example = "2026-10-25T12:00:00") LocalDateTime resueltaEn,
        @Schema(example = "1") Long resueltaPor,
        @Schema(example = "null") String observacion,
        @Schema(example = "4") Long reintegroId) {

    public static SolicitudDevolucionResponse de(SolicitudDevolucion s) {
        return new SolicitudDevolucionResponse(s.getId(), s.getEntrada().getId(), s.getEntrada().getEventoId(),
                s.getUsuarioId(), s.getEntrada().getPrecio(), s.getMotivo(), s.getEstado(), s.getSolicitadaEn(),
                s.getResueltaEn(), s.getResueltaPor(), s.getObservacion(), s.getReintegroId());
    }
}
