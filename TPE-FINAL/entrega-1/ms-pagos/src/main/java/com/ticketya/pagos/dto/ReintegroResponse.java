package com.ticketya.pagos.dto;

import com.ticketya.pagos.model.Reintegro;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReintegroResponse(
        @Schema(example = "4") Long id,
        @Schema(example = "8") Long pagoId,
        @Schema(example = "45000.00") BigDecimal monto,
        @Schema(example = "Devolución aprobada de la entrada 31") String motivo,
        @Schema(example = "APROBADO") String estado,
        @Schema(example = "2026-10-25T10:00:00") LocalDateTime creadoEn) {

    public static ReintegroResponse de(Reintegro r) {
        return new ReintegroResponse(r.getId(), r.getPago().getId(), r.getMonto(), r.getMotivo(), r.getEstado(),
                r.getCreadoEn());
    }
}
