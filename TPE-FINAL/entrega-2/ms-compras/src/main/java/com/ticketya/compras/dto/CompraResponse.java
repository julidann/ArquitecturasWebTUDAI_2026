package com.ticketya.compras.dto;

import com.ticketya.compras.model.Compra;
import com.ticketya.compras.model.EstadoCompra;
import com.ticketya.compras.model.MedioPago;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompraResponse(
        @Schema(example = "21") Long id,
        @Schema(example = "100") Long usuarioId,
        @Schema(example = "15") Long reservaId,
        @Schema(example = "1") Long eventoId,
        @Schema(example = "Los Piojos - Ritual 2026") String nombreEvento,
        @Schema(example = "2") Integer cantidad,
        @Schema(example = "90000.00") BigDecimal montoTotal,
        @Schema(example = "TARJETA_CREDITO") MedioPago medioPago,
        @Schema(example = "3704") String ultimosDigitos,
        @Schema(example = "8") Long pagoId,
        @Schema(example = "APROBADA") EstadoCompra estado,
        @Schema(example = "null", description = "Motivo de rechazo o error, si lo hay") String detalle,
        @Schema(example = "2026-10-20T18:31:00") LocalDateTime creadaEn,
        List<EntradaResponse> entradas) {

    public static CompraResponse de(Compra c) {
        return new CompraResponse(c.getId(), c.getUsuarioId(), c.getReservaId(), c.getEventoId(), c.getNombreEvento(),
                c.getCantidad(), c.getMontoTotal(), c.getMedioPago(), c.getUltimosDigitos(), c.getPagoId(),
                c.getEstado(), c.getDetalle(), c.getCreadaEn(),
                c.getEntradas().stream().map(EntradaResponse::de).toList());
    }
}
