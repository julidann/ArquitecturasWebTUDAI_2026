package com.ticketya.reservas.model;

/**
 * PENDIENTE: lugares bloqueados esperando el pago (vence a los N minutos).
 * CONFIRMADA: el pago se aprobo y los lugares quedaron vendidos.
 * LIBERADA: el pago fue rechazado o el cliente cancelo.
 * VENCIDA: paso el tiempo de reserva sin confirmar.
 */
public enum EstadoReserva {
    PENDIENTE, CONFIRMADA, LIBERADA, VENCIDA
}
