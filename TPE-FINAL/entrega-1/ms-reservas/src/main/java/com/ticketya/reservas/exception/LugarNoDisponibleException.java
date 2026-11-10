package com.ticketya.reservas.exception;

/** Alguno de los asientos pedidos (o el cupo de la zona) ya no esta disponible. Se responde 409. */
public class LugarNoDisponibleException extends RuntimeException {
    public LugarNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
