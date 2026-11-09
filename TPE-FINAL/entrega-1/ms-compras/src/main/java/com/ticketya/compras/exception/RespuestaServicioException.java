package com.ticketya.compras.exception;

/**
 * Otro microservicio respondio con un error 4xx. Se conserva el status y el mensaje
 * para poder devolverselo al cliente tal cual (ej: 409 "la reserva vencio").
 */
public class RespuestaServicioException extends RuntimeException {

    private final int status;

    public RespuestaServicioException(int status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
