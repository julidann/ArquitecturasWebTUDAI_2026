package com.ticketya.compras.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Configuration
public class AppConfig {

    @Value("${servicios.timeout-ms:3000}")
    private int timeoutMs;

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public RestClient reservasHttp(@Value("${servicios.reservas.url}") String url) {
        return crear(url);
    }

    @Bean
    public RestClient pagosHttp(@Value("${servicios.pagos.url}") String url) {
        return crear(url);
    }

    @Bean
    public RestClient eventosHttp(@Value("${servicios.eventos.url}") String url) {
        return crear(url);
    }

    /** Timeouts cortos: si otro servicio no responde preferimos fallar rapido y que el cliente reintente. */
    private RestClient crear(String baseUrl) {
        // cliente HTTP del JDK (el de HttpURLConnection no soporta PATCH)
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory)
                .requestInterceptor(new com.ticketya.compras.trazabilidad.CorrelacionInterceptor()).build();
    }

    @Bean
    public OpenAPI comprasOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa - ms-compras")
                        .version("2.0.0")
                        .description("""
                                Orquesta la compra: valida la reserva, cobra a través de ms-pagos, confirma la
                                reserva en ms-reservas y emite las entradas con su código QR. También gestiona
                                las devoluciones y la cancelación de eventos.

                                `POST /compras` exige el encabezado **Idempotency-Key**: si el cliente reintenta
                                (por ejemplo, por un corte de conexión) con la misma clave, recibe el resultado
                                original y no se le cobra dos veces.

                                **Entrega 2:** control de acceso con QR (`/accesos`), reportes para organizadores
                                (`/reportes`), resiliencia ante la pasarela (reintentos, circuit breaker y fallback)
                                y recuperación automática de compras que quedaron a mitad de camino (saga).
                                Autenticación con JWT en el gateway (`Authorization: Bearer`).
                                """))
                .components(new io.swagger.v3.oas.models.Components().addSecuritySchemes("bearerAuth",
                        new io.swagger.v3.oas.models.security.SecurityScheme()
                                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bearerAuth"))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("A través del API Gateway"),
                        new Server().url("http://localhost:8083").description("Directo al microservicio")));
    }
}
