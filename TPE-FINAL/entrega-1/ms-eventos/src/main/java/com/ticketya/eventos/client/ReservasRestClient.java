package com.ticketya.eventos.client;

import com.ticketya.eventos.exception.ServicioExternoException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class ReservasRestClient implements ReservasClient {

    private final RestClient restClient;

    public ReservasRestClient(@Qualifier("reservasHttp") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void inicializarDisponibilidad(DisponibilidadInicialRequest request) {
        try {
            restClient.post()
                    .uri("/disponibilidad/eventos/{id}", request.eventoId())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ServicioExternoException("No se pudo inicializar la disponibilidad en ms-reservas", e);
        }
    }

    @Override
    public void cambiarHabilitacion(Long eventoId, boolean habilitado, String motivo) {
        try {
            restClient.patch()
                    .uri("/disponibilidad/eventos/{id}/estado", eventoId)
                    .body(Map.of("habilitado", habilitado, "motivo", motivo == null ? "" : motivo))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ServicioExternoException("No se pudo actualizar la disponibilidad en ms-reservas", e);
        }
    }
}
