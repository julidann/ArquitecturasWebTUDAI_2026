package com.ticketya.notificaciones.exception;

/** Otro servicio (o el proveedor de email) no respondio. */
public class ServicioNoDisponibleException extends RuntimeException {
    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
