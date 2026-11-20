package com.ticketya.pagos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pagosOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa - ms-pagos")
                        .version("2.0.0")
                        .description("""
                                Pasarela de pagos **simulada**. Solo la usa ms-compras: el gateway no la expone.
                                Con `PASARELA_TASA_FALLOS` (0 a 1) se simulan caídas (503 sin cobrar) para probar
                                los reintentos, el circuit breaker y la recuperación de compras de ms-compras.

                                Tarjetas de prueba: terminada en `0002` → rechazada (fondos insuficientes),
                                terminada en `0069` → rechazada (tarjeta vencida), cualquier otra → aprobada.
                                """))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .servers(List.of(new Server().url("http://localhost:8084").description("Directo al microservicio")));
    }
}
