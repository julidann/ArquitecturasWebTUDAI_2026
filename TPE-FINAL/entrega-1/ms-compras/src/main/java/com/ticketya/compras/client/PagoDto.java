package com.ticketya.compras.client;

import java.math.BigDecimal;

public record PagoDto(Long id, Long compraId, BigDecimal monto, String estado, String codigoAutorizacion,
                      String motivoRechazo) {

    public boolean aprobado() {
        return "APROBADO".equals(estado);
    }
}
