package com.ticketya.notificaciones.config;

import com.ticketya.notificaciones.trazabilidad.CorrelacionInterceptor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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

    @Value("${servicios.timeout-ms:2000}")
    private int timeoutMs;

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public RestClient usuariosHttp(@Value("${servicios.usuarios.url}") String url) {
        return crear(url);
    }

    @Bean
    public RestClient comprasHttp(@Value("${servicios.compras.url}") String url) {
        return crear(url);
    }

    @Bean
    public RestClient recomendacionesHttp(@Value("${servicios.recomendaciones.url}") String url) {
        return crear(url);
    }

    private RestClient crear(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory)
                .requestInterceptor(new CorrelacionInterceptor()).build();
    }

    @Bean
    public OpenAPI notificacionesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa - ms-notificaciones")
                        .version("2.0.0")
                        .description("""
                                Notificaciones por email (funcionalidad 11): confirmación de compra, cancelación o
                                cambio de fecha de un evento, y aviso cuando un artista favorito se presenta en la
                                ciudad del cliente. Se generan de forma **asincrónica** a partir de los mensajes de
                                RabbitMQ y se guardan en **MongoDB**. El proveedor de email es un mock protegido con
                                reintentos y circuit breaker; lo que no se puede enviar queda PENDIENTE y se reintenta.
                                """))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("A través del API Gateway"),
                        new Server().url("http://localhost:8088").description("Directo al microservicio")));
    }
}
