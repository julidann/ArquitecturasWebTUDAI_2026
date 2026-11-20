package com.ticketya.integracion;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.ticketya.integracion.Api.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Flujo completo a traves de gateway -> eventos -> reservas -> compras -> pagos, y los efectos
 * asincronicos (notificaciones, bitacora, recomendaciones) via RabbitMQ.
 */
class FlujoCompraIT extends IntegracionBase {

    @Test
    void compraCompletaConQrNotificacionBitacoraYReporte() {
        String org = organizador();
        String cliente = clienteNuevo();
        long[] lugar = lugar("Tandil", 3, 5, 50);
        long evento = eventoPublicado(org, lugar, "Recital IT", List.of("Banda IT"), LocalDateTime.now().plusDays(20), false);

        // 1. reserva temporal de 2 asientos
        List<Integer> asientos = asientosLibres(evento, lugar[1]).subList(0, 2);
        Response reserva = reservar(cliente, evento, lugar[1], asientos);
        assertThat(reserva.statusCode()).as(reserva.asString()).isEqualTo(201);
        assertThat(asientosLibres(evento, lugar[1])).doesNotContainAnyElementsOf(asientos);

        // 2. compra (saga: cobro en ms-pagos + confirmacion en ms-reservas + emision de entradas)
        Response compra = comprar(cliente, reserva.<Integer>path("id"));
        assertThat(compra.statusCode()).as(compra.asString()).isEqualTo(201);
        assertThat(compra.<String>path("estado")).isEqualTo("APROBADA");
        List<String> qrs = compra.path("entradas.codigoQr");
        assertThat(qrs).hasSize(2);
        String correlationId = compra.header("X-Correlation-Id");
        assertThat(correlationId).isNotBlank();

        // 3. notificacion de compra confirmada (asincronica)
        await().untilAsserted(() -> assertThat(como(cliente).get("/notificaciones").then().statusCode(200)
                .extract().<List<String>>path("tipo")).contains("COMPRA_CONFIRMADA"));

        // 4. bitacora centralizada: la misma operacion se sigue en 3 servicios con el correlation id
        String admin = admin();
        await().untilAsserted(() -> {
            List<String> servicios = como(admin).get("/bitacora/trazas/" + correlationId).then().statusCode(200)
                    .extract().path("servicio");
            assertThat(servicios).contains("ms-pagos", "ms-reservas", "ms-compras");
        });

        // 5. control de acceso: entra una sola vez
        String acceso = acceso();
        Map<String, Object> qr = Map.of("codigoQr", qrs.get(0), "eventoId", evento, "puerta", "Puerta 1");
        como(acceso).body(qr).post("/accesos/validar").then().statusCode(200);
        assertThat(como(acceso).body(qr).post("/accesos/validar").then().statusCode(409)
                .extract().<String>path("resultado")).isEqualTo("YA_UTILIZADA");

        // 6. reporte del organizador
        Response reporte = como(org).get("/reportes/eventos/" + evento);
        assertThat(reporte.statusCode()).isEqualTo(200);
        assertThat(reporte.<Integer>path("totales.vendidas")).isEqualTo(2);
        assertThat(reporte.<Integer>path("totales.ingresaron")).isEqualTo(1);
        assertThat(reporte.<Float>path("totales.recaudacion")).isEqualTo(40000f);
    }

    @Test
    void conTarjetaRechazadaLaReservaSeLiberaYNoSeEmitenEntradas() {
        String org = organizador();
        String cliente = clienteNuevo();
        long[] lugar = lugar("Tandil", 2, 5, 10);
        long evento = eventoPublicado(org, lugar, "Recital rechazo IT", List.of("Otra banda"), LocalDateTime.now().plusDays(20), false);
        List<Integer> asientos = asientosLibres(evento, lugar[1]).subList(0, 1);
        int reserva = reservar(cliente, evento, lugar[1], asientos).then().statusCode(201).extract().path("id");

        Response compra = como(cliente).header("Idempotency-Key", "rechazo-" + reserva)
                .body(Map.of("reservaId", reserva, "medioPago", Map.of("tipo", "TARJETA_CREDITO",
                        "numeroTarjeta", "4000000000000002", "titular", "Sin fondos", "cuotas", 1)))
                .post("/compras");

        assertThat(compra.statusCode()).isEqualTo(402);
        assertThat(compra.<String>path("estado")).isEqualTo("RECHAZADA");
        assertThat(asientosLibres(evento, lugar[1])).containsAll(asientos); // compensacion: lugares liberados
    }

    @Test
    void lasPreferenciasGeneranRecomendacionesYAvisoDeArtistaFavorito() {
        String cliente = clienteNuevo();
        como(cliente).body(Map.of("tiposEvento", List.of("RECITAL"), "artistasFavoritos", List.of("Artista IT Favorito"),
                "ciudad", "Olavarria")).put("/preferencias").then().statusCode(200);

        long[] lugar = lugar("Olavarria", 1, 5, 10);
        long evento = eventoPublicado(organizador(), lugar, "Show del artista favorito", List.of("Artista IT Favorito"),
                LocalDateTime.now().plusDays(15), false);

        await().untilAsserted(() -> assertThat(como(cliente).get("/notificaciones").then().extract()
                .<List<String>>path("tipo")).contains("ARTISTA_FAVORITO"));
        await().untilAsserted(() -> assertThat(como(cliente).get("/recomendaciones").then().statusCode(200)
                .extract().<List<Integer>>path("eventoId")).contains((int) evento));
    }
}
