package com.ticketya.gateway.seguridad;

import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * Matriz de autorizacion: que roles pueden usar cada endpoint publico del sistema.
 * Es la unica fuente de verdad de los permisos por rol (los microservicios ademas validan reglas
 * de negocio como "solo el organizador del evento" o "la entrada es del cliente").
 *
 * Las reglas se evaluan en orden: gana la primera que coincide. Lo que no esta en la lista
 * requiere al menos estar autenticado.
 */
public final class ReglasDeAcceso {

    public static final String CLIENTE = "CLIENTE";
    public static final String ORGANIZADOR = "ORGANIZADOR";
    public static final String ADMIN = "ADMIN";
    public static final String ACCESO = "ACCESO";

    /** Rutas abiertas a cualquiera (sin token). */
    static final List<Regla> PUBLICAS = List.of(
            new Regla(null, "/actuator/health/**", "/actuator/health", "/actuator/info"),
            new Regla(null, "/swagger-ui.html", "/swagger-ui/**", "/webjars/**", "/v3/api-docs/**", "/docs/**"),
            new Regla(HttpMethod.POST, "/api/auth/login", "/api/auth/registro"),
            new Regla(HttpMethod.GET, "/api/eventos", "/api/eventos/**", "/api/lugares", "/api/lugares/**"),
            new Regla(HttpMethod.GET, "/api/disponibilidad/**"),
            new Regla(HttpMethod.GET, "/api/recomendaciones/eventos/**"),
            // endpoints internos: se dejan pasar para que la ruta "bloquear-internos" responda 404 a todos
            // (asi no se revela que existen)
            new Regla(null, "/api/reservas/*/confirmar", "/api/reservas/*/liberar",
                    "/api/disponibilidad/eventos/*/liberaciones", "/api/internal/**", "/internal/**"),
            new Regla(HttpMethod.OPTIONS, "/**"));

    /** Rutas que exigen alguno de los roles indicados. */
    static final List<ReglaConRoles> CON_ROLES = List.of(
            // usuarios
            new ReglaConRoles(HttpMethod.GET, List.of(CLIENTE, ORGANIZADOR, ADMIN, ACCESO), "/api/usuarios/me"),
            new ReglaConRoles(null, List.of(ADMIN), "/api/usuarios", "/api/usuarios/**"),
            // catalogo
            new ReglaConRoles(HttpMethod.POST, List.of(ADMIN), "/api/eventos/*/suspender", "/api/eventos/*/reanudar",
                    "/api/eventos/*/cancelar"),
            new ReglaConRoles(HttpMethod.POST, List.of(ORGANIZADOR, ADMIN), "/api/lugares", "/api/eventos",
                    "/api/eventos/*/publicar", "/api/eventos/*/reprogramar"),
            new ReglaConRoles(HttpMethod.PUT, List.of(ORGANIZADOR, ADMIN), "/api/eventos/*", "/api/eventos/*/precios"),
            // reservas y compras
            new ReglaConRoles(null, List.of(CLIENTE), "/api/reservas", "/api/reservas/**"),
            new ReglaConRoles(HttpMethod.POST, List.of(CLIENTE), "/api/compras"),
            new ReglaConRoles(HttpMethod.GET, List.of(CLIENTE, ADMIN), "/api/compras", "/api/compras/**"),
            new ReglaConRoles(null, List.of(CLIENTE), "/api/entradas", "/api/entradas/**"),
            // devoluciones
            new ReglaConRoles(HttpMethod.POST, List.of(ADMIN), "/api/devoluciones/*/aprobar",
                    "/api/devoluciones/*/rechazar"),
            new ReglaConRoles(HttpMethod.GET, List.of(CLIENTE, ADMIN), "/api/devoluciones"),
            // control de acceso
            new ReglaConRoles(HttpMethod.POST, List.of(ACCESO), "/api/accesos/validar"),
            new ReglaConRoles(HttpMethod.GET, List.of(ACCESO, ORGANIZADOR, ADMIN), "/api/accesos/**"),
            // reportes
            new ReglaConRoles(null, List.of(ORGANIZADOR, ADMIN), "/api/reportes/**"),
            // fila virtual
            new ReglaConRoles(HttpMethod.PUT, List.of(ORGANIZADOR, ADMIN), "/api/fila/eventos/*/configuracion"),
            new ReglaConRoles(HttpMethod.GET, List.of(ORGANIZADOR, ADMIN), "/api/fila/eventos/*/estado"),
            new ReglaConRoles(null, List.of(CLIENTE), "/api/fila/**"),
            // preferencias y recomendaciones
            new ReglaConRoles(null, List.of(CLIENTE), "/api/preferencias", "/api/preferencias/**", "/api/recomendaciones"),
            // notificaciones (cada uno ve las suyas)
            new ReglaConRoles(null, List.of(CLIENTE, ORGANIZADOR, ADMIN, ACCESO), "/api/notificaciones", "/api/notificaciones/**"),
            // log centralizado
            new ReglaConRoles(null, List.of(ADMIN), "/api/bitacora", "/api/bitacora/**"));

    private ReglasDeAcceso() {
    }

    /** method == null significa cualquier metodo. */
    record Regla(HttpMethod method, String... patrones) {
    }

    record ReglaConRoles(HttpMethod method, List<String> roles, String... patrones) {
    }
}
