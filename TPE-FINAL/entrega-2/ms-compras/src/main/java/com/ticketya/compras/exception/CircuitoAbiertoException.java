package com.ticketya.compras.exception;

/**
 * El circuit breaker esta abierto: ni siquiera se llamo al otro servicio. No tiene sentido
 * reintentar enseguida (por eso el Retry la ignora), se falla rapido.
 */
public class CircuitoAbiertoException extends ServicioNoDisponibleException {
    public CircuitoAbiertoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
