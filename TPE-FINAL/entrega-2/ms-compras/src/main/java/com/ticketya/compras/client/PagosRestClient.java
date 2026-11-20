package com.ticketya.compras.client;

import com.ticketya.compras.dto.DatosMedioPago;
import com.ticketya.compras.exception.CircuitoAbiertoException;
import com.ticketya.compras.exception.ServicioNoDisponibleException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Cliente de la pasarela de pagos (sistema externo) con resiliencia (Resilience4j, ver application.yml):
 *  - timeout: el RestClient corta a los 3 s (servicios.timeout-ms).
 *  - retry: hasta 3 intentos con espera exponencial, SOLO ante fallas de comunicacion o 5xx. Es seguro
 *    reintentar un cobro porque siempre se manda la misma Idempotency-Key ("compra-{id}"): si el primer
 *    intento llego a cobrar, la pasarela devuelve ese mismo pago.
 *  - circuit breaker: si falla mas del 50% de las ultimas llamadas se "abre" 15 s y se falla rapido sin
 *    llamar (no se acumulan threads esperando a una pasarela caida).
 *  - fallback: la compra queda en ERROR y el job de recuperacion la retoma (o el cliente reintenta).
 */
@Component
public class PagosRestClient implements PagosClient {

    private static final String SERVICIO = "ms-pagos";
    private final RestClient restClient;

    public PagosRestClient(@Qualifier("pagosHttp") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @Retry(name = "pagos")
    @CircuitBreaker(name = "pagos", fallbackMethod = "pagarFallback")
    public PagoDto pagar(Long compraId, Long usuarioId, BigDecimal monto, DatosMedioPago medioPago,
                         String idempotencyKey) {
        Map<String, Object> body = Map.of(
                "compraId", compraId,
                "usuarioId", usuarioId,
                "monto", monto,
                "medioPago", medioPago);
        return ErroresHttp.llamar(SERVICIO, () -> restClient.post()
                .uri("/pagos")
                .header("Idempotency-Key", idempotencyKey)
                .body(body)
                .retrieve()
                .body(PagoDto.class));
    }

    @Override
    @Retry(name = "pagos")
    @CircuitBreaker(name = "pagos", fallbackMethod = "reintegrarFallback")
    public ReintegroDto reintegrar(Long pagoId, BigDecimal monto, String motivo, String idempotencyKey) {
        return ErroresHttp.llamar(SERVICIO, () -> restClient.post()
                .uri("/pagos/{id}/reintegros", pagoId)
                .header("Idempotency-Key", idempotencyKey)
                .body(Map.of("monto", monto, "motivo", motivo == null ? "" : motivo))
                .retrieve()
                .body(ReintegroDto.class));
    }

    @Override
    @Retry(name = "pagos")
    @CircuitBreaker(name = "pagos", fallbackMethod = "buscarFallback")
    public List<PagoDto> buscarPorCompra(Long compraId) {
        return ErroresHttp.llamar(SERVICIO, () -> restClient.get()
                .uri("/pagos?compraId={id}", compraId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<PagoDto>>() {
                }));
    }

    // ---- fallbacks: se traduce todo a ServicioNoDisponibleException (las 4xx se propagan tal cual) ----

    @SuppressWarnings("unused")
    private PagoDto pagarFallback(Long compraId, Long usuarioId, BigDecimal monto, DatosMedioPago medioPago,
                                  String idempotencyKey, Throwable t) {
        throw traducir(t);
    }

    @SuppressWarnings("unused")
    private ReintegroDto reintegrarFallback(Long pagoId, BigDecimal monto, String motivo, String idempotencyKey,
                                            Throwable t) {
        throw traducir(t);
    }

    @SuppressWarnings("unused")
    private List<PagoDto> buscarFallback(Long compraId, Throwable t) {
        throw traducir(t);
    }

    static RuntimeException traducir(Throwable t) {
        if (t instanceof CallNotPermittedException) {
            return new CircuitoAbiertoException("La pasarela de pagos no está disponible (circuito abierto)", t);
        }
        return t instanceof RuntimeException r ? r : new ServicioNoDisponibleException(SERVICIO + " falló", t);
    }
}
