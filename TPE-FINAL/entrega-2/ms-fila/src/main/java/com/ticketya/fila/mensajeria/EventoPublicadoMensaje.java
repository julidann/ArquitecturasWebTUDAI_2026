package com.ticketya.fila.mensajeria;

/** Lo que nos interesa del mensaje evento.publicado que emite ms-eventos. */
public record EventoPublicadoMensaje(Long eventoId, String nombre, Long organizadorId, boolean altaDemanda) {
}
