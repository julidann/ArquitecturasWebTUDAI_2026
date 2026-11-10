package com.ticketya.reservas.concurrencia;

import com.ticketya.reservas.ReservasApplication;
import com.ticketya.reservas.dto.ReservaRequest;
import com.ticketya.reservas.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Misma prueba pero contra PostgreSQL (la base que usamos de verdad). Ademas se levanta
 * una SEGUNDA instancia completa de ms-reservas apuntando a la misma base, y los clientes
 * se reparten entre las dos instancias: asi se verifica que el mecanismo no depende de
 * nada en memoria del proceso.
 *
 * Se saltea automaticamente si no hay Docker disponible.
 */
@SpringBootTest
@Import(RelojDePrueba.class)
@Testcontainers(disabledWithoutDocker = true)
class NoDobleVentaPostgresTest extends NoDobleVentaTestBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void dosInstanciasDelServicioCompitenPorElMismoAsientoYSoloSeVendeUnaVez() throws Exception {
        long eventoId = publicarEvento(10);

        try (ConfigurableApplicationContext segundaInstancia = new SpringApplicationBuilder(ReservasApplication.class)
                // se pasan como argumentos de linea de comando para que pisen el application.yml de test
                .run("--server.port=0",
                        "--spring.datasource.url=" + postgres.getJdbcUrl(),
                        "--spring.datasource.username=" + postgres.getUsername(),
                        "--spring.datasource.password=" + postgres.getPassword(),
                        "--spring.jpa.hibernate.ddl-auto=none",
                        "--reservas.intervalo-vencimiento-ms=3600000")) {
            ReservaService instancia2 = segundaInstancia.getBean(ReservaService.class);

            Resultado r = ejecutarEnParalelo(CLIENTES_SIMULTANEOS, usuario -> {
                ReservaService destino = usuario % 2 == 0 ? reservaService : instancia2;
                return destino.crear(usuario, new ReservaRequest(eventoId, 1L, List.of(5L), null));
            });

            assertThat(r.exitos()).isEqualTo(1);
            assertThat(r.rechazos()).isEqualTo(CLIENTES_SIMULTANEOS - 1);
        }
    }
}
