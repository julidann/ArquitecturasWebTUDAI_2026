package com.ticketya.fila.config;

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
        return Clock.systemUTC();
    }

    @Bean
    public OpenAPI filaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa - ms-fila")
                        .version("2.0.0")
                        .description("""
                                Fila virtual para eventos de **alta demanda** (funcionalidad 6). Al abrirse la venta
                                el cliente ingresa a la fila y consulta su posición; cada N segundos se habilita una
                                tanda de clientes en orden de llegada. El cliente habilitado recibe un **turno**
                                firmado que tiene que mandar en el header `X-Turno-Fila` al reservar en ms-reservas.
                                """))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("A través del API Gateway"),
                        new Server().url("http://localhost:8086").description("Directo al microservicio")));
    }
}
