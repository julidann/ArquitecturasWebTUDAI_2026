package com.ticketya.pagos.exception;

/** Se reutilizo una Idempotency-Key con datos distintos a los del pedido original. */
public class ClaveIdempotenciaReutilizadaException extends RuntimeException {
    public ClaveIdempotenciaReutilizadaException(String mensaje) {
        super(mensaje);
    }
}
