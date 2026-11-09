package com.ticketya.compras.client;

public interface ReservasClient {

    ReservaDto obtener(Long reservaId);

    ReservaDto confirmar(Long reservaId);

    void liberar(Long reservaId);

    /** Devuelve al inventario un lugar vendido (devolucion aprobada). */
    void liberarVendido(Long eventoId, Long reservaId, Long sectorId, Long asientoId, String idempotencyKey);
}
