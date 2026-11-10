package com.ticketya.reservas.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Avisos "best effort" a ms-eventos. Si falla solo se loguea: que el evento figure como
 * AGOTADO es informativo, la disponibilidad real siempre se controla en este servicio.
 */
@Component
public class EventosRestClient implements EventosClient {

    private static final Logger log = LoggerFactory.getLogger(EventosRestClient.class);

    private final RestClient restClient;

    public EventosRestClient(@Value("${servicios.eventos.url}") String url,
                             @Value("${servicios.timeout-ms:2000}") int timeoutMs) {
        // cliente HTTP del JDK (el de HttpURLConnection no soporta PATCH)
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    @Override
    public void marcarAgotado(Long eventoId) {
        avisar("/internal/eventos/{id}/agotado", eventoId);
    }

    @Override
    public void marcarDisponible(Long eventoId) {
        avisar("/internal/eventos/{id}/disponible", eventoId);
    }

    private void avisar(String uri, Long eventoId) {
        try {
            restClient.post().uri(uri, eventoId).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("No se pudo avisar a ms-eventos ({}) para el evento {}: {}", uri, eventoId, e.getMessage());
        }
    }
}
