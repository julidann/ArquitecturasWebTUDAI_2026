package com.ticketya.bitacora.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public OpenAPI bitacoraOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa - ms-bitacora")
                        .version("2.0.0")
                        .description("""
                                Log centralizado (consigna de trazabilidad). Cada microservicio publica sus
                                operaciones críticas (compra, devolución, cancelación, ingreso al evento, cobros...)
                                en RabbitMQ y este servicio las guarda en **MongoDB**. Con el `X-Correlation-Id` que
                                genera el gateway se puede seguir una misma operación a través de todos los servicios.
                                """))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("A través del API Gateway"),
                        new Server().url("http://localhost:8089").description("Directo al microservicio")));
    }
}

