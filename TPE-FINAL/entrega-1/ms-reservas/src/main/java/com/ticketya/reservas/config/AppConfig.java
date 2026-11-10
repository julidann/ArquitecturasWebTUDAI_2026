package com.ticketya.reservas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

@Configuration
public class AppConfig {

    /** Se inyecta un Clock para poder simular el paso del tiempo en los tests de vencimiento. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public OpenAPI reservasOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa - ms-reservas")
                        .version("1.0.0")
                        .description("""
                                Disponibilidad de lugares por evento y reservas temporales.

                                Es el servicio responsable de que **una entrada nunca se venda dos veces**: los
                                asientos y cupos se toman con UPDATEs condicionales en la base de datos, por lo
                                que el mecanismo funciona aun con varias instancias del servicio.

                                Una reserva queda PENDIENTE durante 10 minutos (configurable); si no se confirma
                                el pago en ese tiempo pasa a VENCIDA y los lugares se liberan.
                                """))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("A través del API Gateway"),
                        new Server().url("http://localhost:8082").description("Directo al microservicio")));
    }
}
