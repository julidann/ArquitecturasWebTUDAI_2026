package com.ticketya.reservas.client;

public interface EventosClient {

    void marcarAgotado(Long eventoId);

    void marcarDisponible(Long eventoId);
}
