package com.ticketya.pagos.service;

/** La pasarela externa no respondio: no se cobro nada y el pedido se puede reintentar con la misma clave. */
public class PasarelaNoDisponibleException extends RuntimeException {
    public PasarelaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
