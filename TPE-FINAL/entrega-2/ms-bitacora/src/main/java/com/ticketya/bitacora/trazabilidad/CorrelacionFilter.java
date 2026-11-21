package com.ticketya.bitacora.trazabilidad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Bitacora de requests HTTP: registra cada request con endpoint, usuario, codigo de respuesta y
 * tiempo de procesamiento. Ademas toma el X-Correlation-Id que pone el gateway (o genera uno) y
 * lo deja en el MDC, asi todas las lineas de log de esa operacion lo incluyen y se propaga a las
 * llamadas a otros servicios (ver CorrelacionInterceptor) y a los mensajes de RabbitMQ.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelacionFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_CLAVE = "correlationId";
    private static final Logger log = LoggerFactory.getLogger("bitacora.http");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long inicio = System.currentTimeMillis();
        String id = request.getHeader(HEADER);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        MDC.put(MDC_CLAVE, id);
        response.setHeader(HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            String usuario = request.getHeader("X-Usuario-Id");
            String rol = request.getHeader("X-Rol");
            log.info("{} {} usuario={} rol={} status={} tiempo={}ms", request.getMethod(), request.getRequestURI(),
                    usuario == null ? "anonimo" : usuario, rol == null ? "-" : rol, response.getStatus(),
                    System.currentTimeMillis() - inicio);
            MDC.remove(MDC_CLAVE);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator") || uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui");
    }

    /** Id de la operacion en curso (o uno nuevo si se llama fuera de un request, por ejemplo desde un job). */
    public static String actual() {
        String id = MDC.get(MDC_CLAVE);
        return id == null ? UUID.randomUUID().toString() : id;
    }
}
