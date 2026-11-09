package com.ticketya.eventos.client;

public interface ReservasClient {

    void inicializarDisponibilidad(DisponibilidadInicialRequest request);

    void cambiarHabilitacion(Long eventoId, boolean habilitado, String motivo);
}
