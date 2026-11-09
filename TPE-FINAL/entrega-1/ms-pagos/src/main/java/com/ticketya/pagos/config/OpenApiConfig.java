package com.ticketya.pagos.config;

import io.swagger.v3.oas.models.OpenAPI;
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
                        .version("1.0.0")
                        .description("""
                                Pasarela de pagos **simulada** para la entrega 1. Lo usa ms-compras (no se expone
                                al público a través del gateway salvo para consultas).

                                Tarjetas de prueba: terminada en `0002` → rechazada (fondos insuficientes),
                                terminada en `0069` → rechazada (tarjeta vencida), cualquier otra → aprobada.
                                """))
                .servers(List.of(new Server().url("http://localhost:8084").description("Directo al microservicio")));
    }
}
