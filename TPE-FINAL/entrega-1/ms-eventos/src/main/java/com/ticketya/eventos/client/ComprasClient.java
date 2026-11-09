package com.ticketya.eventos.client;

public interface ComprasClient {

    /** Avisa a ms-compras que el evento se cancelo para que pase las entradas a reintegro. */
    void notificarCancelacion(Long eventoId, String motivo);
}
