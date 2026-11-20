package com.ticketya.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Levanta el gateway real con todos los microservicios reemplazados por un WireMock que responde
 * 200 a cualquier pedido. Asi se prueba solo lo que hace el gateway: autenticacion, autorizacion
 * por rol, ruteo y headers que reenvia.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "jwt.secreto=" + Tokens.SECRETO)
abstract class GatewayTestBase {

    static final WireMockServer servicios = new WireMockServer(options().dynamicPort());

    static {
        servicios.start();
    }

    @Autowired
    WebTestClient client;

    @DynamicPropertySource
    static void urls(DynamicPropertyRegistry registry) {
        for (String s : new String[]{"eventos", "reservas", "compras", "pagos", "usuarios", "fila", "recomendaciones",
                "notificaciones", "bitacora"}) {
            registry.add("servicios." + s, servicios::baseUrl);
        }
    }

    @BeforeEach
    void stubs() {
        servicios.resetAll();
        servicios.stubFor(any(anyUrl()).willReturn(okJson("{\"ok\":true}")));
    }

    @AfterAll
    static void fin() {
        // el server se comparte entre las clases de test (mismo contexto de Spring)
    }
}
