package com.ticketya.compras.model;

/**
 * PENDIENTE: se esta procesando el pago.
 * APROBADA: pago aprobado y entradas emitidas.
 * RECHAZADA: pago rechazado (o reserva vencida, con reintegro del dinero).
 * ERROR: fallo la comunicacion con otro servicio; se puede reintentar con la misma Idempotency-Key.
 */
public enum EstadoCompra {
    PENDIENTE, APROBADA, RECHAZADA, ERROR
}
