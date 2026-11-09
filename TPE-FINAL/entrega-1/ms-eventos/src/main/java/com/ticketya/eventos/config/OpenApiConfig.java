package com.ticketya.eventos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eventosOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa - ms-eventos")
                        .version("1.0.0")
                        .description("""
                                Catálogo de lugares y eventos: alta de lugares con sus sectores, creación de eventos
                                en borrador, definición de precios por sector, publicación, suspensión/cancelación
                                y búsqueda por múltiples criterios.

                                **Entrega 1:** no hay autenticación. El usuario se identifica con los encabezados
                                `X-Usuario-Id` y `X-Rol` (CLIENTE, ORGANIZADOR, ADMIN, ACCESO).
                                """))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("A través del API Gateway"),
                        new Server().url("http://localhost:8081").description("Directo al microservicio")));
    }
}
