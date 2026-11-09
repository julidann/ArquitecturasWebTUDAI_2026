package com.ticketya.pagos.dto;

import com.ticketya.pagos.model.EstadoPago;
import com.ticketya.pagos.model.MedioPago;
import com.ticketya.pagos.model.Pago;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PagoResponse(
        @Schema(example = "8") Long id,
        @Schema(example = "21") Long compraId,
        @Schema(example = "90000.00") BigDecimal monto,
        @Schema(example = "TARJETA_CREDITO") MedioPago medioPago,
        @Schema(example = "3704") String ultimosDigitos,
        @Schema(example = "APROBADO") EstadoPago estado,
        @Schema(example = "AUT-7F3K9Q") String codigoAutorizacion,
        @Schema(example = "null") String motivoRechazo,
        @Schema(example = "0.00") BigDecimal totalReintegrado,
        @Schema(example = "2026-10-20T18:31:05") LocalDateTime creadoEn,
        List<ReintegroResponse> reintegros) {

    public static PagoResponse de(Pago p) {
        return new PagoResponse(p.getId(), p.getCompraId(), p.getMonto(), p.getMedioPago(), p.getUltimosDigitos(),
                p.getEstado(), p.getCodigoAutorizacion(), p.getMotivoRechazo(), p.totalReintegrado(), p.getCreadoEn(),
                p.getReintegros().stream().map(ReintegroResponse::de).toList());
    }
}
