package com.ticketya.integracion;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.ticketya.integracion.Api.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seguridad de punta a punta con tokens reales de ms-usuarios (la matriz completa de endpoints x
 * roles se prueba en el gateway: SeguridadEndpointsTest).
 */
class SeguridadIT extends IntegracionBase {

    @Test
    void sinTokenSoloSeAccedeALoPublico() {
        given().get("/eventos").then().statusCode(200);
        given().get("/compras").then().statusCode(401);
        given().get("/notificaciones").then().statusCode(401);
    }

    @Test
    void cadaRolSoloAccedeASusEndpoints() {
        String cliente = clienteNuevo();
        como(cliente).get("/bitacora").then().statusCode(403);
        como(cliente).get("/usuarios").then().statusCode(403);
        como(cliente).body(Map.of("codigoQr", "x", "eventoId", 1)).post("/accesos/validar").then().statusCode(403);
        como(acceso()).get("/compras").then().statusCode(403);
        como(organizador()).post("/reservas").then().statusCode(403);
        como(admin()).get("/bitacora").then().statusCode(200);
        como(admin()).get("/usuarios").then().statusCode(200);
    }

    @Test
    void elRegistroPublicoSiempreCreaClientesAunqueSePidaOtroRol() {
        String token = clienteNuevo();
        assertThat(como(token).get("/usuarios/me").then().statusCode(200).extract().<String>path("rol"))
                .isEqualTo("CLIENTE");
    }

    @Test
    void noSePuedeFalsificarLaIdentidadConHeaders() {
        String cliente = clienteNuevo();
        // intenta hacerse pasar por ADMIN mandando los headers que usan los microservicios
        como(cliente).header("X-Rol", "ADMIN").header("X-Usuario-Id", "1").get("/bitacora").then().statusCode(403);
        given().header("X-Rol", "ADMIN").header("X-Usuario-Id", "1").get("/compras").then().statusCode(401);
    }

    @Test
    void losEndpointsInternosNoSeExponen() {
        como(admin()).get("/internal/eventos/1/compradores").then().statusCode(404);
        como(admin()).post("/reservas/1/confirmar").then().statusCode(404);
    }

    @Test
    void credencialesInvalidasDan401() {
        given().contentType("application/json").body(Map.of("email", "admin@ticketya.com", "password", "mal"))
                .post("/auth/login").then().statusCode(401);
        given().header("Authorization", "Bearer token-trucho").get("/compras").then().statusCode(401);
    }
}
