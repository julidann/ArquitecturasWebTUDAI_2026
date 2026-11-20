package com.ticketya.integracion;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/** Cliente del API Gateway para los tests: login, alta de datos y llamadas con el JWT. */
public final class Api {

    public static final String GATEWAY = System.getProperty("gateway.url", "http://localhost:8080");
    private static final AtomicInteger SECUENCIA = new AtomicInteger();
    private static final String CORRIDA = UUID.randomUUID().toString().substring(0, 8);

    static {
        RestAssured.baseURI = GATEWAY;
        RestAssured.basePath = "/api";
    }

    private Api() {
    }

    public static boolean gatewayDisponible() {
        try {
            return given().basePath("").get("/actuator/health").statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public static String login(String email, String password) {
        return given().contentType(ContentType.JSON).body(Map.of("email", email, "password", password))
                .post("/auth/login").then().statusCode(200).extract().path("token");
    }

    public static String admin() {
        return login("admin@ticketya.com", "admin123");
    }

    public static String organizador() {
        return login("organizador@ticketya.com", "organizador123");
    }

    public static String acceso() {
        return login("acceso@ticketya.com", "acceso123");
    }

    /** Registra un cliente nuevo (cada test usa los suyos) y devuelve su token. */
    public static String clienteNuevo() {
        String email = "it-" + CORRIDA + "-" + SECUENCIA.incrementAndGet() + "@mail.com";
        given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "nombre", "Cliente de prueba", "password", "cliente123"))
                .post("/auth/registro").then().statusCode(201);
        return login(email, "cliente123");
    }

    public static RequestSpecification como(String token) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON);
    }

    public static String fecha(LocalDateTime f) {
        return f.withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /** Lugar con un sector numerado de filas x asientos y un campo; devuelve {lugarId, sectorNumerado, sectorCampo}. */
    public static long[] lugar(String ciudad, int filas, int asientos, int campo) {
        Response r = como(admin()).body(Map.of(
                        "nombre", "Lugar IT " + CORRIDA + "-" + SECUENCIA.incrementAndGet(),
                        "direccion", "Calle falsa 123", "ciudad", ciudad, "latitud", -37.32, "longitud", -59.13,
                        "sectores", List.of(
                                Map.of("nombre", "Platea", "tipo", "NUMERADO", "filas", filas, "asientosPorFila", asientos),
                                Map.of("nombre", "Campo", "tipo", "NO_NUMERADO", "capacidad", campo))))
                .post("/lugares");
        assertThat(r.statusCode()).as(r.asString()).isEqualTo(201);
        List<Integer> sectores = r.path("sectores.id");
        return new long[]{r.<Integer>path("id"), sectores.get(0), sectores.get(1)};
    }

    /** Crea, pone precios y publica un evento del organizador de prueba. Devuelve el id. */
    public static long eventoPublicado(String org, long[] lugar, String nombre, List<String> artistas,
                                       LocalDateTime fecha, boolean altaDemanda) {
        Response r = como(org).body(Map.of("nombre", nombre, "descripcion", "Evento de los tests de integracion",
                        "tipo", "RECITAL", "participantes", artistas, "fechaHora", fecha(fecha), "lugarId", lugar[0],
                        "altaDemanda", altaDemanda, "limiteEntradasPorCliente", 6))
                .post("/eventos");
        assertThat(r.statusCode()).as(r.asString()).isEqualTo(201);
        long id = r.<Integer>path("id");
        como(org).body(Map.of("precios", List.of(Map.of("sectorId", lugar[1], "precio", 20000),
                Map.of("sectorId", lugar[2], "precio", 10000)))).put("/eventos/" + id + "/precios")
                .then().statusCode(200);
        como(org).post("/eventos/" + id + "/publicar").then().statusCode(200);
        return id;
    }

    /** Ids de los asientos disponibles de un sector. */
    public static List<Integer> asientosLibres(long eventoId, long sectorId) {
        return given().get("/disponibilidad/eventos/" + eventoId + "/sectores/" + sectorId + "/asientos")
                .then().statusCode(200).extract().path("findAll { it.estado == 'DISPONIBLE' }.asientoId");
    }

    public static Response reservar(String token, long eventoId, long sectorId, List<Integer> asientos) {
        return como(token).body(Map.of("eventoId", eventoId, "sectorId", sectorId, "asientoIds", asientos))
                .post("/reservas");
    }

    public static Response comprar(String token, long reservaId) {
        return como(token).header("Idempotency-Key", UUID.randomUUID().toString())
                .body(Map.of("reservaId", reservaId, "medioPago", Map.of("tipo", "TARJETA_CREDITO",
                        "numeroTarjeta", "4509953566233704", "titular", "Cliente IT", "cuotas", 1)))
                .post("/compras");
    }
}
