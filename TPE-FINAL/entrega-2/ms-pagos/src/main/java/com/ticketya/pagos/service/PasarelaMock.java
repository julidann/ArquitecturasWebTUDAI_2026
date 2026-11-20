package com.ticketya.pagos.service;

import com.ticketya.pagos.dto.DatosMedioPago;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simula la pasarela de pagos externa (la consigna pide un mock para la entrega 1).
 *
 * Reglas fijas para poder probar los dos caminos de la compra:
 *  - tarjeta terminada en 0002 -> rechazada por fondos insuficientes
 *  - tarjeta terminada en 0069 -> rechazada por tarjeta vencida
 *  - monto mayor al limite configurado -> rechazado
 *  - cualquier otro caso -> aprobado
 * Ademas tiene una demora configurable para simular la latencia de un servicio real y (entrega 2)
 * una tasa de fallos: en ese porcentaje de pedidos la pasarela "no esta disponible" y no cobra nada,
 * para poder probar la resiliencia de ms-compras (reintentos, circuit breaker y recuperacion).
 */
@Component
public class PasarelaMock {

    private final long demoraMs;
    private final BigDecimal montoMaximo;
    private volatile double tasaFallos;

    public PasarelaMock(@Value("${pasarela.demora-ms:300}") long demoraMs,
                        @Value("${pasarela.monto-maximo:5000000}") BigDecimal montoMaximo,
                        @Value("${pasarela.tasa-fallos:0}") double tasaFallos) {
        this.demoraMs = demoraMs;
        this.montoMaximo = montoMaximo;
        this.tasaFallos = tasaFallos;
    }

    public Resultado cobrar(BigDecimal monto, DatosMedioPago medio) {
        esperar();
        simularCaida();
        if (monto.compareTo(montoMaximo) > 0) {
            return Resultado.rechazado("El monto supera el límite permitido por la pasarela");
        }
        if (medio.tipo().esTarjeta()) {
            String numero = medio.numeroTarjeta();
            if (numero.endsWith("0002")) {
                return Resultado.rechazado("Fondos insuficientes");
            }
            if (numero.endsWith("0069")) {
                return Resultado.rechazado("Tarjeta vencida");
            }
        }
        return Resultado.aprobado("AUT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
    }

    public void reintegrar(BigDecimal monto) {
        esperar();
        simularCaida();
    }

    public double getTasaFallos() {
        return tasaFallos;
    }

    /** Permite cambiar la tasa en caliente (lo usan los tests). */
    public void setTasaFallos(double tasaFallos) {
        if (tasaFallos < 0 || tasaFallos > 1) {
            throw new IllegalArgumentException("La tasa de fallos va de 0 a 1");
        }
        this.tasaFallos = tasaFallos;
    }

    private void simularCaida() {
        if (tasaFallos > 0 && ThreadLocalRandom.current().nextDouble() < tasaFallos) {
            throw new PasarelaNoDisponibleException("La pasarela externa no responde (falla simulada)");
        }
    }

    private void esperar() {
        if (demoraMs <= 0) {
            return;
        }
        try {
            Thread.sleep(demoraMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public record Resultado(boolean aprobado, String codigoAutorizacion, String motivoRechazo) {
        static Resultado aprobado(String codigo) {
            return new Resultado(true, codigo, null);
        }

        static Resultado rechazado(String motivo) {
            return new Resultado(false, null, motivo);
        }
    }
}
