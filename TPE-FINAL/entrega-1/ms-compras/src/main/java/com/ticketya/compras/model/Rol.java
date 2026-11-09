package com.ticketya.compras.model;

/**
 * Roles del sistema. En la entrega 1 llegan por el header X-Rol (sin autenticacion);
 * en la entrega 2 se van a sacar del token JWT.
 */
public enum Rol {
    CLIENTE, ORGANIZADOR, ADMIN, ACCESO;

    public static Rol desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return CLIENTE;
        }
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol invalido: " + valor);
        }
    }
}
