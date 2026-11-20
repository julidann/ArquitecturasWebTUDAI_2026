package com.ticketya.eventos.config;

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
    public OpenAPI eventosOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa - ms-eventos")
                        .version("2.0.0")
                        .description("""
                                Catálogo de lugares y eventos: alta de lugares con sus sectores, creación de eventos
                                en borrador, definición de precios por sector, publicación, suspensión/cancelación
                                y búsqueda por múltiples criterios.

                                **Entrega 2:** autenticación con JWT (`Authorization: Bearer ...`, se obtiene en
                                `POST /api/auth/login`). Las consultas (GET) son públicas; crear y modificar es para
                                ORGANIZADOR (dueño del evento) o ADMIN, y suspender / cancelar solo ADMIN. El gateway
                                valida el token y le pasa al servicio la identidad en `X-Usuario-Id` / `X-Rol`.

                                El detalle, las búsquedas y los lugares se cachean en Redis (ver docs/06). Al
                                publicar, cancelar o reprogramar se emite un mensaje de dominio a RabbitMQ (outbox).
                                """))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("A través del API Gateway"),
                        new Server().url("http://localhost:8081").description("Directo al microservicio")));
    }
}
