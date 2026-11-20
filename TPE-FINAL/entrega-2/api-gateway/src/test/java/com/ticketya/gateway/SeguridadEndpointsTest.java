package com.ticketya.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Pruebas de seguridad de endpoints: para CADA endpoint publico del sistema y CADA rol (mas el
 * usuario anonimo) se verifica que solo los roles correspondientes llegan al microservicio.
 * Sin token -> 401, rol no permitido -> 403, rol permitido -> pasa (200 del WireMock).
 */
class SeguridadEndpointsTest extends GatewayTestBase {

    static final String ANONIMO = "ANONIMO";
    static final List<String> ROLES = List.of(ANONIMO, "CLIENTE", "ORGANIZADOR", "ADMIN", "ACCESO");
    static final Set<String> PUBLICO = Set.of(ANONIMO, "CLIENTE", "ORGANIZADOR", "ADMIN", "ACCESO");
    static final Set<String> AUTENTICADOS = Set.of("CLIENTE", "ORGANIZADOR", "ADMIN", "ACCESO");
    static final Set<String> CLIENTE = Set.of("CLIENTE");
    static final Set<String> ORG_ADMIN = Set.of("ORGANIZADOR", "ADMIN");
    static final Set<String> ADMIN = Set.of("ADMIN");
    static final Set<String> CLIENTE_ADMIN = Set.of("CLIENTE", "ADMIN");

    /** Matriz de permisos esperada (la misma que se documenta en docs/04-api-rest.md). */
    static List<Object[]> matriz() {
        return List.of(
                // autenticacion y usuarios
                fila("POST", "/api/auth/login", PUBLICO),
                fila("POST", "/api/auth/registro", PUBLICO),
                fila("GET", "/api/usuarios/me", AUTENTICADOS),
                fila("GET", "/api/usuarios", ADMIN),
                fila("POST", "/api/usuarios", ADMIN),
                fila("POST", "/api/usuarios/7/deshabilitar", ADMIN),
                // lugares y eventos
                fila("GET", "/api/lugares", PUBLICO),
                fila("GET", "/api/lugares/1", PUBLICO),
                fila("GET", "/api/lugares/1/sectores/2/asientos", PUBLICO),
                fila("POST", "/api/lugares", ORG_ADMIN),
                fila("GET", "/api/eventos", PUBLICO),
                fila("GET", "/api/eventos/1", PUBLICO),
                fila("POST", "/api/eventos", ORG_ADMIN),
                fila("PUT", "/api/eventos/1", ORG_ADMIN),
                fila("PUT", "/api/eventos/1/precios", ORG_ADMIN),
                fila("POST", "/api/eventos/1/publicar", ORG_ADMIN),
                fila("POST", "/api/eventos/1/reprogramar", ORG_ADMIN),
                fila("POST", "/api/eventos/1/suspender", ADMIN),
                fila("POST", "/api/eventos/1/reanudar", ADMIN),
                fila("POST", "/api/eventos/1/cancelar", ADMIN),
                // disponibilidad, reservas y compras
                fila("GET", "/api/disponibilidad/eventos/1", PUBLICO),
                fila("GET", "/api/disponibilidad/eventos/1/sectores/2/asientos", PUBLICO),
                fila("POST", "/api/reservas", CLIENTE),
                fila("GET", "/api/reservas", CLIENTE),
                fila("GET", "/api/reservas/1", CLIENTE),
                fila("DELETE", "/api/reservas/1", CLIENTE),
                fila("POST", "/api/compras", CLIENTE),
                fila("GET", "/api/compras", CLIENTE_ADMIN),
                fila("GET", "/api/compras/1", CLIENTE_ADMIN),
                fila("GET", "/api/entradas", CLIENTE),
                fila("GET", "/api/entradas/1", CLIENTE),
                fila("GET", "/api/entradas/1/qr", CLIENTE),
                fila("POST", "/api/entradas/1/devolucion", CLIENTE),
                // devoluciones
                fila("GET", "/api/devoluciones", CLIENTE_ADMIN),
                fila("POST", "/api/devoluciones/1/aprobar", ADMIN),
                fila("POST", "/api/devoluciones/1/rechazar", ADMIN),
                // control de acceso y reportes
                fila("POST", "/api/accesos/validar", Set.of("ACCESO")),
                fila("GET", "/api/accesos/eventos/1", Set.of("ACCESO", "ORGANIZADOR", "ADMIN")),
                fila("GET", "/api/reportes/eventos/1", ORG_ADMIN),
                // fila virtual
                fila("POST", "/api/fila/eventos/1/ingresar", CLIENTE),
                fila("GET", "/api/fila/eventos/1/mi-turno", CLIENTE),
                fila("DELETE", "/api/fila/eventos/1", CLIENTE),
                fila("PUT", "/api/fila/eventos/1/configuracion", ORG_ADMIN),
                fila("GET", "/api/fila/eventos/1/estado", ORG_ADMIN),
                // preferencias, recomendaciones, notificaciones, bitacora
                fila("GET", "/api/preferencias", CLIENTE),
                fila("PUT", "/api/preferencias", CLIENTE),
                fila("GET", "/api/recomendaciones", CLIENTE),
                fila("GET", "/api/recomendaciones/eventos/1/tambien-compraron", PUBLICO),
                fila("GET", "/api/notificaciones", AUTENTICADOS),
                fila("GET", "/api/bitacora", ADMIN));
    }

    static Stream<Arguments> casos() {
        List<Arguments> casos = new ArrayList<>();
        for (Object[] f : matriz()) {
            @SuppressWarnings("unchecked")
            Set<String> permitidos = (Set<String>) f[2];
            for (String rol : ROLES) {
                casos.add(Arguments.of(f[0], f[1], rol, permitidos.contains(rol)));
            }
        }
        return casos.stream();
    }

    @ParameterizedTest(name = "{0} {1} como {2} -> permitido={3}")
    @MethodSource("casos")
    void cadaEndpointSoloEsAccesiblePorSusRoles(String metodo, String path, String rol, boolean permitido) {
        var spec = client.method(HttpMethod.valueOf(metodo)).uri(path).contentType(MediaType.APPLICATION_JSON);
        if (!rol.equals(ANONIMO)) {
            spec = spec.header("Authorization", "Bearer " + Tokens.de(50, rol));
        }
        var respuesta = (metodo.equals("GET") || metodo.equals("DELETE") ? spec : spec.bodyValue("{}")).exchange();

        if (permitido) {
            respuesta.expectStatus().isOk();
        } else if (rol.equals(ANONIMO)) {
            respuesta.expectStatus().isUnauthorized().expectBody().jsonPath("$.status").isEqualTo(401);
        } else {
            respuesta.expectStatus().isForbidden().expectBody().jsonPath("$.status").isEqualTo(403);
        }
        servicios.verify(permitido ? 1 : 0, anyRequestedFor(anyUrl()));
    }

    @Test
    @DisplayName("El cliente no puede hacerse pasar por otro usuario o por admin mandando los headers a mano")
    void reemplazaLosHeadersDeIdentidadPorLosDelToken() {
        client.post().uri("/api/reservas")
                .header("Authorization", "Bearer " + Tokens.de(4, "CLIENTE"))
                .header("X-Usuario-Id", "999")
                .header("X-Rol", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                .exchange().expectStatus().isOk();

        servicios.verify(postRequestedFor(urlEqualTo("/reservas"))
                .withHeader("X-Usuario-Id", equalTo("4"))
                .withHeader("X-Rol", equalTo("CLIENTE"))
                .withHeader("X-Usuario-Email", equalTo("u4@test.com")));
    }

    @Test
    void enLasRutasPublicasSinTokenSeBorranLosHeadersDeIdentidad() {
        client.get().uri("/api/eventos").header("X-Usuario-Id", "1").header("X-Rol", "ADMIN")
                .exchange().expectStatus().isOk();
        servicios.verify(getRequestedFor(urlEqualTo("/eventos")).withoutHeader("X-Usuario-Id").withoutHeader("X-Rol"));
    }

    @Test
    void unTokenVencidoDa401() {
        client.get().uri("/api/reservas").header("Authorization", "Bearer " + Tokens.vencido(4, "CLIENTE"))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void unTokenFirmadoConOtraClaveDa401() {
        client.get().uri("/api/reservas").header("Authorization", "Bearer " + Tokens.conOtraClave(1, "ADMIN"))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void unTokenDeOtroEmisorDa401() {
        String token = Tokens.firmar(1, "ADMIN", Tokens.SECRETO, java.time.Instant.now().plusSeconds(600), "otro");
        client.get().uri("/api/bitacora").header("Authorization", "Bearer " + token)
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void unTokenMalformadoDa401() {
        client.get().uri("/api/reservas").header("Authorization", "Bearer no-es-un-jwt")
                .exchange().expectStatus().isUnauthorized();
    }

    private static Object[] fila(String metodo, String path, Set<String> roles) {
        return new Object[]{metodo, path, roles};
    }
}
