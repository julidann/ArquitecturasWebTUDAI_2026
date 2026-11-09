package com.ticketya.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayApplicationTest {

    @Autowired
    private WebTestClient client;

    @Test
    void noExponeLosEndpointsInternosDeReservas() {
        client.post().uri("/api/reservas/1/confirmar").exchange().expectStatus().isNotFound();
        client.post().uri("/api/disponibilidad/eventos/1/liberaciones").exchange().expectStatus().isNotFound();
    }

    @Test
    void noExponeLaInicializacionDeDisponibilidad() {
        client.post().uri("/api/disponibilidad/eventos/1").exchange().expectStatus().isNotFound();
    }

    @Test
    void agregaElCorrelationIdALaRespuesta() {
        client.get().uri("/actuator/health").exchange()
                .expectStatus().isOk();
        client.post().uri("/api/reservas/1/confirmar").exchange()
                .expectHeader().exists("X-Correlation-Id");
    }
}
