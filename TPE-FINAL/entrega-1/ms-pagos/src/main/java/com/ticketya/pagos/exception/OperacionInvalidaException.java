package com.ticketya.pagos.exception;

/** Regla de negocio violada (ej: publicar un evento sin precios). Se responde con 409. */
public class OperacionInvalidaException extends RuntimeException {
    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
