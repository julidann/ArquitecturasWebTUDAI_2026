package com.ticketya.compras.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Reporte de ventas de un evento (funcionalidad 12)")
public record ReporteEventoResponse(
        @Schema(example = "1") Long eventoId,
        @Schema(example = "Los Piojos - Ritual 2026") String nombreEvento,
        @Schema(example = "10") Long organizadorId,
        LocalDateTime generadoEn,
        @Schema(description = "false si ms-reservas no respondió: falta la capacidad y la ocupación") boolean ocupacionDisponible,
        List<Sector> sectores,
        Totales totales) {

    public record Sector(
            @Schema(example = "3") Long sectorId,
            @Schema(example = "Platea Baja") String nombre,
            @Schema(description = "Capacidad del sector", example = "200") Integer capacidad,
            @Schema(description = "Entradas emitidas (incluye las que después se devolvieron o cancelaron)", example = "150") long vendidas,
            @Schema(example = "4") long devueltas,
            @Schema(description = "Cancelaciones por cancelación del evento", example = "0") long canceladas,
            @Schema(description = "Entradas vigentes (válidas + ya utilizadas)", example = "146") long vigentes,
            @Schema(description = "Personas que ya ingresaron con esa entrada", example = "0") long ingresaron,
            @Schema(description = "vigentes / capacidad", example = "73.0") Double porcentajeOcupacion,
            @Schema(description = "Recaudación neta (sin lo reintegrado)", example = "6570000.00") BigDecimal recaudacion,
            @Schema(description = "Monto reintegrado por devoluciones y cancelaciones", example = "180000.00") BigDecimal reintegrado) {
    }

    public record Totales(Integer capacidad, long vendidas, long devueltas, long canceladas, long vigentes,
                          long ingresaron, Double porcentajeOcupacion, BigDecimal recaudacion, BigDecimal reintegrado) {
    }
}
