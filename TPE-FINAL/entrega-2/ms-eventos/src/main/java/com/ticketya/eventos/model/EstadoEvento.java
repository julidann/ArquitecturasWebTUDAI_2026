package com.ticketya.eventos.model;

/**
 * Estados posibles de un evento. SUSPENDIDO no figura en la consigna como estado,
 * pero lo agregamos porque los administradores pueden suspender un evento
 * (a diferencia de cancelar, una suspension se puede revertir).
 */
public enum EstadoEvento {
    BORRADOR, PUBLICADO, AGOTADO, SUSPENDIDO, CANCELADO, FINALIZADO;

    public boolean admiteVentas() {
        return this == PUBLICADO;
    }
}
