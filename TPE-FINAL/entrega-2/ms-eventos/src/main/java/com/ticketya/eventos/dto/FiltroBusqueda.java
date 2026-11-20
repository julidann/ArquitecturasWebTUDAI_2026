package com.ticketya.eventos.dto;

import com.ticketya.eventos.model.TipoEvento;

import java.time.LocalDate;

/** Agrupa los criterios de busqueda para no pasar 10 parametros sueltos al service. */
public record FiltroBusqueda(
        String texto,
        TipoEvento tipo,
        Long organizadorId,
        String participante,
        String ciudad,
        LocalDate desde,
        LocalDate hasta,
        OrdenBusqueda orden,
        boolean descendente,
        int pagina,
        int tamanio) {
}
