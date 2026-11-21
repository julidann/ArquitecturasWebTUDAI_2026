package com.ticketya.notificaciones.service;

import com.ticketya.notificaciones.exception.ServicioNoDisponibleException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Simula el proveedor externo de email/push (SendGrid, Firebase, etc.): escribe el mensaje en el log.
 * Tiene una tasa de fallos configurable para probar la resiliencia. Llamada protegida con
 * reintentos + circuit breaker: si el proveedor esta caido no seguimos insistiendo en cada mensaje,
 * la notificacion queda PENDIENTE y el job la reintenta mas tarde (fallback).
 */
@Component
public class ProveedorNotificaciones {

    private static final Logger log = LoggerFactory.getLogger("notificaciones.email");

    private volatile double tasaFallos;

    public ProveedorNotificaciones(@Value("${proveedor.tasa-fallos:0}") double tasaFallos) {
        this.tasaFallos = tasaFallos;
    }

    @Retry(name = "proveedor")
    @CircuitBreaker(name = "proveedor", fallbackMethod = "noDisponible")
    public void enviar(String destino, String asunto, String cuerpo) {
        if (tasaFallos > 0 && ThreadLocalRandom.current().nextDouble() < tasaFallos) {
            throw new ServicioNoDisponibleException("El proveedor de email no responde (falla simulada)", null);
        }
        log.info("[EMAIL] para={} asunto=\"{}\" cuerpo=\"{}\"", destino, asunto, cuerpo);
    }

    @SuppressWarnings("unused")
    private void noDisponible(String destino, String asunto, String cuerpo, Throwable t) {
        if (t instanceof CallNotPermittedException) {
            throw new ServicioNoDisponibleException("Proveedor de email no disponible (circuito abierto)", t);
        }
        throw t instanceof RuntimeException r ? r : new ServicioNoDisponibleException("Falló el envío", t);
    }

    public void setTasaFallos(double tasaFallos) {
        this.tasaFallos = tasaFallos;
    }
}
