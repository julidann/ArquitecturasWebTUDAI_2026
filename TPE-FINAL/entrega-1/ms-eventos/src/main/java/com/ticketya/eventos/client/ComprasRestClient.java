package com.ticketya.eventos.client;

import com.ticketya.eventos.exception.ServicioExternoException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class ComprasRestClient implements ComprasClient {

    private final RestClient restClient;

    public ComprasRestClient(@Qualifier("comprasHttp") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void notificarCancelacion(Long eventoId, String motivo) {
        try {
            restClient.post()
                    .uri("/internal/eventos/{id}/cancelacion", eventoId)
                    .body(Map.of("motivo", motivo == null ? "" : motivo))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ServicioExternoException("No se pudo notificar la cancelacion a ms-compras", e);
        }
    }
}
