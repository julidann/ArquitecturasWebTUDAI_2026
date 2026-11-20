package com.ticketya.integracion;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.ticketya.integracion.Api.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Cancelacion y reprogramacion: consistencia entre eventos, compras, pagos y notificaciones. */
class CancelacionYCambioIT extends IntegracionBase {

    private long[] lugar;
    private long evento;
    private String cliente;

    private void eventoConUnaCompra(String nombre) {
        lugar = lugar("Tandil", 2, 5, 10);
        evento = eventoPublicado(organizador(), lugar, nombre, List.of("Banda IT"), LocalDateTime.now().plusDays(25), false);
        cliente = clienteNuevo();
        int reserva = reservar(cliente, evento, lugar[1], asientosLibres(evento, lugar[1]).subList(0, 2))
                .then().statusCode(201).extract().path("id");
        comprar(cliente, reserva).then().statusCode(201);
    }

    @Test
    void alCancelarElEventoSeCancelanLasEntradasSeReintegraYSeAvisa() {
        eventoConUnaCompra("Recital a cancelar IT");

        como(admin()).body(Map.of("motivo", "Tormenta")).post("/eventos/" + evento + "/cancelar").then().statusCode(200);

        assertThat(como(cliente).get("/entradas").then().extract()
                .<List<String>>path("findAll { it.eventoId == " + evento + " }.estado")).containsOnly("CANCELADA");
        await().untilAsserted(() -> assertThat(como(cliente).get("/notificaciones").then().extract()
                .<List<String>>path("tipo")).contains("EVENTO_CANCELADO"));
        // ya no se puede reservar
        assertThat(reservar(clienteNuevo(), evento, lugar[1], asientosLibres(evento, lugar[1]).subList(0, 1))
                .statusCode()).isIn(409, 422);
    }

    @Test
    void alReprogramarSeAvisaALosCompradoresYLasEntradasSiguenValidas() {
        eventoConUnaCompra("Recital a reprogramar IT");
        LocalDateTime nueva = LocalDateTime.now().plusDays(50).withHour(21).withMinute(0).withSecond(0).withNano(0);

        como(organizador()).body(Map.of("fechaHora", fecha(nueva), "motivo", "Cambio de agenda"))
                .post("/eventos/" + evento + "/reprogramar").then().statusCode(200);

        await().untilAsserted(() -> assertThat(como(cliente).get("/notificaciones").then().extract()
                .<List<String>>path("tipo")).contains("EVENTO_REPROGRAMADO"));
        assertThat(como(cliente).get("/entradas").then().extract()
                .<List<String>>path("findAll { it.eventoId == " + evento + " }.estado")).containsOnly("VALIDA");
        assertThat(io.restassured.RestAssured.get("/disponibilidad/eventos/" + evento).then().extract()
                .<String>path("fechaEvento")).startsWith(fecha(nueva).substring(0, 16));
    }
}
