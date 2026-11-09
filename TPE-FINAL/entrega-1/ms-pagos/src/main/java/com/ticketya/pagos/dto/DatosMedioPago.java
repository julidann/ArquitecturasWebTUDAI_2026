package com.ticketya.pagos.dto;

import com.ticketya.pagos.model.MedioPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = """
        Datos del medio de pago. En el mock, una tarjeta terminada en 0002 se rechaza por fondos
        insuficientes y una terminada en 0069 por tarjeta vencida. Cualquier otra se aprueba.""")
public record DatosMedioPago(
        @NotNull @Schema(example = "TARJETA_CREDITO") MedioPago tipo,
        @Pattern(regexp = "\\d{13,19}", message = "debe tener entre 13 y 19 digitos")
        @Schema(example = "4509953566233704") String numeroTarjeta,
        @Schema(example = "Juan Perez") String titular,
        @Min(1) @Max(12) @Schema(example = "1") Integer cuotas) {
}
