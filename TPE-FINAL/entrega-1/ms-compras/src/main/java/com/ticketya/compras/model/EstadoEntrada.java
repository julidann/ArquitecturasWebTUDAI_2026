package com.ticketya.compras.model;

/**
 * VALIDA: se puede usar para ingresar.
 * DEVUELTA: el cliente la devolvio y se le reintegro el dinero.
 * CANCELADA: el evento se cancelo y se reintegro el dinero.
 * UTILIZADA: ya se uso para ingresar (control de acceso, entrega 2).
 */
public enum EstadoEntrada {
    VALIDA, DEVUELTA, CANCELADA, UTILIZADA
}
