package com.ticketya.compras.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Component
public class ReservasRestClient implements ReservasClient {

    private static final String SERVICIO = "ms-reservas";
    private final RestClient restClient;

    public ReservasRestClient(@Qualifier("reservasHttp") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public ReservaDto obtener(Long reservaId) {
        return ErroresHttp.llamar(SERVICIO, () -> restClient.get()
                .uri("/reservas/{id}", reservaId)
                .retrieve()
                .body(ReservaDto.class));
    }

    @Override
    public ReservaDto confirmar(Long reservaId) {
        return ErroresHttp.llamar(SERVICIO, () -> restClient.post()
                .uri("/reservas/{id}/confirmar", reservaId)
                .retrieve()
                .body(ReservaDto.class));
    }

    @Override
    public void liberar(Long reservaId) {
        ErroresHttp.llamar(SERVICIO, () -> restClient.post()
                .uri("/reservas/{id}/liberar", reservaId)
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public void liberarVendido(Long eventoId, Long reservaId, Long sectorId, Long asientoId, String idempotencyKey) {
        Map<String, Object> body = new HashMap<>();
        body.put("reservaId", reservaId);
        body.put("sectorId", sectorId);
        body.put("asientoId", asientoId);
        body.put("cantidad", 1);
        ErroresHttp.llamar(SERVICIO, () -> restClient.post()
                .uri("/disponibilidad/eventos/{id}/liberaciones", eventoId)
                .header("Idempotency-Key", idempotencyKey)
                .body(body)
                .retrieve()
                .toBodilessEntity());
    }
}
