package com.ticketya.gateway.filtro;

import com.ticketya.gateway.seguridad.IdentidadFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Bitacora de requests: registra CADA request que llega al gateway (incluidos los rechazados por
 * seguridad con 401/403) con endpoint, usuario, codigo de respuesta y tiempo de procesamiento.
 * Ademas agrega un X-Correlation-Id (si no viene) que se propaga a los microservicios y a los
 * mensajes de RabbitMQ; con ese id se sigue una operacion en el log centralizado (ms-bitacora).
 *
 * Es un WebFilter (y no un GlobalFilter del gateway) para que corra antes que Spring Security.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogRequestFilter implements WebFilter {

    public static final String CORRELATION_ID = "X-Correlation-Id";
    private static final Logger log = LoggerFactory.getLogger("bitacora.http");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long inicio = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank() || correlationId.length() > 100) {
            correlationId = UUID.randomUUID().toString();
        }
        String id = correlationId;
        ServerHttpRequest conId = Headers.modificar(request, h -> h.set(CORRELATION_ID, id));
        exchange.getResponse().getHeaders().set(CORRELATION_ID, id);
        ServerWebExchange mutado = exchange.mutate().request(conId).build();

        return chain.filter(mutado)
                .doFinally(signal -> {
                    String path = request.getURI().getRawPath();
                    if (path.startsWith("/actuator")) {
                        return;
                    }
                    Object usuario = mutado.getAttributes().get(IdentidadFilter.ATRIBUTO_USUARIO);
                    var status = exchange.getResponse().getStatusCode();
                    log.info("{} {} usuario={} status={} tiempo={}ms correlationId={}",
                            request.getMethod(), path, usuario == null ? "anonimo" : usuario,
                            status == null ? "-" : status.value(), System.currentTimeMillis() - inicio, id);
                });
    }
}
