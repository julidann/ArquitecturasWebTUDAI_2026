package com.ticketya.pagos.model;

public enum MedioPago {
    TARJETA_CREDITO, TARJETA_DEBITO, MERCADO_PAGO;

    public boolean esTarjeta() {
        return this != MERCADO_PAGO;
    }
}
