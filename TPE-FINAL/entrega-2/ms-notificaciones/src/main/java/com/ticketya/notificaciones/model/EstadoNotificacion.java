package com.ticketya.notificaciones.model;

public enum EstadoNotificacion {
    /** todavia no se pudo entregar (el job la reintenta) */
    PENDIENTE,
    ENVIADA,
    /** se agotaron los reintentos */
    FALLIDA,
    /** el usuario no existe o esta deshabilitado */
    DESCARTADA
}
