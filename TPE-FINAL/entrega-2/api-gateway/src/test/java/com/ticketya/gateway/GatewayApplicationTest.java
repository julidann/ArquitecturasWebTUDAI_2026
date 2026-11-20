package com.ticketya.gateway;

import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

class GatewayApplicationTest extends GatewayTestBase {

    @Test
    void noExponeLosEndpointsInternosDeReservas() {
        String token = Tokens.de(4, "CLIENTE");
        client.post().uri("/api/reservas/1/confirmar").header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isNotFound();
        client.post().uri("/api/reservas/1/liberar").exchange().expectStatus().isNotFound();
        client.post().uri("/api/disponibilidad/eventos/1/liberaciones").exchange().expectStatus().isNotFound();
        client.get().uri("/api/internal/eventos/1/compradores").header("Authorization", "Bearer " + Tokens.de(1, "ADMIN"))
                .exchange().expectStatus().isNotFound();
        servicios.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void noExponeLaInicializacionDeDisponibilidad() {
        client.post().uri("/api/disponibilidad/eventos/1").header("Authorization", "Bearer " + Tokens.de(1, "ADMIN"))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void agregaElCorrelationIdALaRespuestaYLoPropaga() {
        client.get().uri("/api/eventos").exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Correlation-Id");
        client.get().uri("/api/eventos").header("X-Correlation-Id", "prueba-123").exchange()
                .expectHeader().valueEquals("X-Correlation-Id", "prueba-123");
        servicios.verify(getRequestedFor(urlEqualTo("/eventos")).withHeader("X-Correlation-Id", equalTo("prueba-123")));
    }

    @Test
    void elHealthEsPublico() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }
}
