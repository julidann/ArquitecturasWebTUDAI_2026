package com.ticketya.usuarios.model;

/** Roles del sistema. Viajan en el claim "rol" del JWT. */
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
