package com.ticketya.compras.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cliente de ms-eventos (lo usan los reportes para saber quien es el organizador del evento).
 * Fallback: si ms-eventos no responde se usa el ultimo dato conocido del evento (el organizador de un
 * evento no cambia, asi que un dato "viejo" sirve igual).
 */
@Component
public class EventosRestClient implements EventosClient {

    private static final Logger log = LoggerFactory.getLogger(EventosRestClient.class);
    private static final String SERVICIO = "ms-eventos";

    private final RestClient restClient;
    private final Map<Long, EventoDto> ultimosConocidos = new ConcurrentHashMap<>();

    public EventosRestClient(@Qualifier("eventosHttp") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @Retry(name = "eventos")
    @CircuitBreaker(name = "eventos", fallbackMethod = "ultimoConocido")
    public EventoDto obtener(Long eventoId) {
        EventoDto e = ErroresHttp.llamar(SERVICIO, () -> restClient.get()
                .uri("/eventos/{id}", eventoId)
                .retrieve()
                .body(EventoDto.class));
        ultimosConocidos.put(eventoId, e);
        return e;
    }

    @SuppressWarnings("unused")
    private EventoDto ultimoConocido(Long eventoId, Throwable t) {
        EventoDto e = ultimosConocidos.get(eventoId);
        if (e == null || t instanceof com.ticketya.compras.exception.RespuestaServicioException) {
            throw PagosRestClient.traducir(t);
        }
        log.warn("ms-eventos no disponible ({}): se usa el ultimo dato conocido del evento {}", t.getMessage(), eventoId);
        return e;
    }
}
