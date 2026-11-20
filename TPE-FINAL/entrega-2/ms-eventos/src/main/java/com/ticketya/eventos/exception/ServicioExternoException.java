package com.ticketya.eventos.exception;

/** Fallo al comunicarse con otro microservicio. */
public class ServicioExternoException extends RuntimeException {
    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
