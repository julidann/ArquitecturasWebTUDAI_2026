package com.ticketya.eventos.dto;

public enum OrdenBusqueda {
    FECHA, PRECIO, RELEVANCIA;

    public static OrdenBusqueda desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return FECHA;
        }
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Orden invalido: " + valor + ". Valores posibles: fecha, precio, relevancia");
        }
    }
}
