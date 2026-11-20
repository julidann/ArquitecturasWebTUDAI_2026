package com.ticketya.compras.client;

import java.math.BigDecimal;

public record ReintegroDto(Long id, Long pagoId, BigDecimal monto, String estado) {
}
