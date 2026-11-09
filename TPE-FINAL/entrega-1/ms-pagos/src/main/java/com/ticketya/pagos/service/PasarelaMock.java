package com.ticketya.pagos.service;

import com.ticketya.pagos.dto.DatosMedioPago;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simula la pasarela de pagos externa (la consigna pide un mock para la entrega 1).
 *
 * Reglas fijas para poder probar los dos caminos de la compra:
 *  - tarjeta terminada en 0002 -> rechazada por fondos insuficientes
 *  - tarjeta terminada en 0069 -> rechazada por tarjeta vencida
 *  - monto mayor al limite configurado -> rechazado
 *  - cualquier otro caso -> aprobado
 * Ademas tiene una demora configurable para simular la latencia de un servicio real.
 */
@Component
public class PasarelaMock {

    private final long demoraMs;
    private final BigDecimal montoMaximo;

    public PasarelaMock(@Value("${pasarela.demora-ms:300}") long demoraMs,
                        @Value("${pasarela.monto-maximo:5000000}") BigDecimal montoMaximo) {
        this.demoraMs = demoraMs;
        this.montoMaximo = montoMaximo;
    }

    public Resultado cobrar(BigDecimal monto, DatosMedioPago medio) {
        esperar();
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
