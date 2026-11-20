package com.ticketya.compras.exception;

/** Pedido valido pero que no cumple una regla (ej: plazo de devolucion vencido). Se responde 422. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
