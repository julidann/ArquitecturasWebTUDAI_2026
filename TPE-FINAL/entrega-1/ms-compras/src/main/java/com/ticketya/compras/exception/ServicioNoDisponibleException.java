package com.ticketya.compras.exception;

/** Otro microservicio no respondio (timeout, conexion rechazada o error 5xx). */
public class ServicioNoDisponibleException extends RuntimeException {
    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
