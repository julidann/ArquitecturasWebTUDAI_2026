package com.ticketya.recomendaciones.config;

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
    public OpenAPI recomendacionesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa - ms-recomendaciones")
                        .version("2.0.0")
                        .description("""
                                Preferencias de los clientes (tipos de evento, artistas favoritos, ciudad) y
                                recomendaciones de eventos (funcionalidad 10). Guarda los datos en **MongoDB**:
                                las preferencias son listas de largo variable y el historial de compras se
                                consulta con *aggregation pipelines* ("quienes compraron este evento también
                                compraron..."). Se alimenta de forma asincrónica con los mensajes
                                `evento.publicado`, `evento.cancelado`, `evento.reprogramado` y `compra.confirmada`.
                                """))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("A través del API Gateway"),
                        new Server().url("http://localhost:8087").description("Directo al microservicio")));
    }
}
