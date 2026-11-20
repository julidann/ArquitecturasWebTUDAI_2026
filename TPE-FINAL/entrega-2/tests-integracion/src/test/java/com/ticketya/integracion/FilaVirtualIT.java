package com.ticketya.integracion;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.ticketya.integracion.Api.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Fila virtual: ms-eventos publica (RabbitMQ) -> ms-fila abre la fila -> turno firmado -> ms-reservas lo valida. */
class FilaVirtualIT extends IntegracionBase {

    @Test
    void enUnEventoDeAltaDemandaSoloReservaQuienTieneTurno() {
        String org = organizador();
        long[] lugar = lugar("Buenos Aires", 2, 10, 100);
        long evento = eventoPublicado(org, lugar, "Recital alta demanda IT", List.of("Banda masiva"),
                LocalDateTime.now().plusDays(30), true);
        String cliente = clienteNuevo();
        List<Integer> asiento = asientosLibres(evento, lugar[1]).subList(0, 1);

        // sin turno no se puede reservar
        assertThat(reservar(cliente, evento, lugar[1], asiento).statusCode()).isEqualTo(403);

        // la fila se abre sola al publicar (mensaje asincronico); tandas de 1 cada 1 segundo para el test
        await().until(() -> como(org).get("/fila/eventos/" + evento + "/estado").statusCode() == 200);
        como(org).body(Map.of("tamanioTanda", 1, "intervaloSegundos", 1, "minutosParaComprar", 5, "activa", true))
                .put("/fila/eventos/" + evento + "/configuracion").then().statusCode(200);

        como(cliente).post("/fila/eventos/" + evento + "/ingresar").then().statusCode(200);
        String turno = await().atMost(Duration.ofSeconds(30)).until(
                () -> como(cliente).get("/fila/eventos/" + evento + "/mi-turno").then().extract().path("turno"),
                t -> t != null);

        // el turno es personal: otro cliente no lo puede usar
        assertThat(reservar(clienteNuevo(), evento, lugar[1], asiento).statusCode()).isEqualTo(403);
        Response otro = como(clienteNuevo()).header("X-Turno-Fila", turno)
                .body(Map.of("eventoId", evento, "sectorId", lugar[1], "asientoIds", asiento)).post("/reservas");
        assertThat(otro.statusCode()).isEqualTo(403);

        Response conTurno = como(cliente).header("X-Turno-Fila", turno)
                .body(Map.of("eventoId", evento, "sectorId", lugar[1], "asientoIds", asiento)).post("/reservas");
        assertThat(conTurno.statusCode()).as(conTurno.asString()).isEqualTo(201);
    }
}
