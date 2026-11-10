package com.ticketya.reservas.exception;

/** El cliente supera el maximo de entradas permitidas para el evento. Se responde 422. */
public class LimiteExcedidoException extends RuntimeException {
    public LimiteExcedidoException(String mensaje) {
        super(mensaje);
    }
}
