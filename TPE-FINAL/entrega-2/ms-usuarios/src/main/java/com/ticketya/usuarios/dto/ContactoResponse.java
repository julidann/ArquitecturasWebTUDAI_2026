package com.ticketya.usuarios.dto;

/** Datos de contacto que necesita ms-notificaciones para enviar un email. */
public record ContactoResponse(Long id, String email, String nombre, boolean activo) {
}
