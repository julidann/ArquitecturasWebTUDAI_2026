package com.ticketya.compras.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.ticketya.compras.dto.DatosMedioPago;
import com.ticketya.compras.exception.RespuestaServicioException;
import com.ticketya.compras.exception.ServicioNoDisponibleException;
import com.ticketya.compras.model.MedioPago;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Resiliencia del cliente REAL de la pasarela contra un ms-pagos simulado con WireMock:
 * reintentos con la misma Idempotency-Key, timeout, circuit breaker y fallback.
 */
@SpringBootTest(properties = "servicios.timeout-ms=500")
class ResilienciaPagosTest {

    static final WireMockServer pagos = new WireMockServer(options().dynamicPort());

    static {
        pagos.start();
    }

    @Autowired
    PagosClient cliente;
    @Autowired
    CircuitBreakerRegistry circuitos;

    static final DatosMedioPago TARJETA = new DatosMedioPago(MedioPago.TARJETA_CREDITO, "4509953566233704", "Juan", 1);
    static final String APROBADO = """
            {"id": 77, "compraId": 1, "monto": 1000, "estado": "APROBADO", "codigoAutorizacion": "AUT-1"}""";

    @DynamicPropertySource
    static void url(DynamicPropertyRegistry r) {
        r.add("servicios.pagos.url", pagos::baseUrl);
    }

    @AfterAll
    static void fin() {
        pagos.stop();
    }

    @BeforeEach
    void reset() {
        pagos.resetAll();
        circuitos.circuitBreaker("pagos").reset();
    }

    @Test
    void anteErrores503SeReintentaConLaMismaClaveHastaQueResponde() {
        pagos.stubFor(post("/pagos").inScenario("caida").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(serviceUnavailable()).willSetStateTo("segundo"));
        pagos.stubFor(post("/pagos").inScenario("caida").whenScenarioStateIs("segundo")
                .willReturn(serviceUnavailable()).willSetStateTo("ok"));
        pagos.stubFor(post("/pagos").inScenario("caida").whenScenarioStateIs("ok")
                .willReturn(okJson(APROBADO)));

        PagoDto pago = cliente.pagar(1L, 4L, BigDecimal.valueOf(1000), TARJETA, "compra-1");

        assertThat(pago.aprobado()).isTrue();
        pagos.verify(3, postRequestedFor(urlEqualTo("/pagos")).withHeader("Idempotency-Key", equalTo("compra-1")));
    }

    @Test
    void unTimeoutTambienSeReintenta() {
        pagos.stubFor(post("/pagos").inScenario("lenta").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(okJson(APROBADO).withFixedDelay(1500)).willSetStateTo("rapida"));
        pagos.stubFor(post("/pagos").inScenario("lenta").whenScenarioStateIs("rapida").willReturn(okJson(APROBADO)));

        PagoDto pago = cliente.pagar(1L, 4L, BigDecimal.valueOf(1000), TARJETA, "compra-1");

        assertThat(pago.id()).isEqualTo(77L);
        pagos.verify(2, postRequestedFor(urlEqualTo("/pagos")));
    }

    @Test
    void lasRespuestas4xxNoSeReintentan() {
        pagos.stubFor(post("/pagos").willReturn(aResponse().withStatus(422)
                .withHeader("Content-Type", "application/json").withBody("{\"mensaje\": \"clave reutilizada\"}")));

        assertThatThrownBy(() -> cliente.pagar(1L, 4L, BigDecimal.TEN, TARJETA, "compra-1"))
                .isInstanceOf(RespuestaServicioException.class);
        pagos.verify(1, postRequestedFor(urlEqualTo("/pagos")));
    }

    @Test
    void conLaPasarelaCaidaElCircuitoSeAbreYSeFallaRapidoSinLlamarla() {
        pagos.stubFor(post("/pagos").willReturn(serviceUnavailable()));

        // 2 compras x 3 intentos: a la 4ta falla se supera el umbral (50% de 4) y el circuito se abre
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> cliente.pagar(1L, 4L, BigDecimal.TEN, TARJETA, "compra-1"))
                    .isInstanceOf(ServicioNoDisponibleException.class);
        }
        assertThat(circuitos.circuitBreaker("pagos").getState().name()).isEqualTo("OPEN");
        int llamadas = pagos.findAll(postRequestedFor(urlEqualTo("/pagos"))).size();

        assertThatThrownBy(() -> cliente.pagar(1L, 4L, BigDecimal.TEN, TARJETA, "compra-1"))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .hasMessageContaining("circuito abierto");
        assertThat(pagos.findAll(postRequestedFor(urlEqualTo("/pagos")))).hasSize(llamadas);
    }
}
