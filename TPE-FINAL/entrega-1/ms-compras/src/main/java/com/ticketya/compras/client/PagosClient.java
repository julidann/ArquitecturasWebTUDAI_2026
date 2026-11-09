package com.ticketya.compras.client;

import com.ticketya.compras.dto.DatosMedioPago;

import java.math.BigDecimal;

public interface PagosClient {

    PagoDto pagar(Long compraId, Long usuarioId, BigDecimal monto, DatosMedioPago medioPago, String idempotencyKey);

    ReintegroDto reintegrar(Long pagoId, BigDecimal monto, String motivo, String idempotencyKey);
}
