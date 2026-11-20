package com.ticketya.integracion;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;

import java.time.Duration;

abstract class IntegracionBase {

    @BeforeAll
    static void sistemaLevantado() {
        Assumptions.assumeTrue(Api.gatewayDisponible(),
                "El gateway no responde en " + Api.GATEWAY + ": levantar el sistema con docker compose up -d --build");
        Awaitility.setDefaultPollInterval(Duration.ofMillis(500));
        Awaitility.setDefaultTimeout(Duration.ofSeconds(30));
    }
}
