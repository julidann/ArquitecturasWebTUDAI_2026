package com.ticketya.gateway.filtro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Registra cada request que pasa por el gateway con endpoint, usuario, codigo de respuesta
 * y tiempo de procesamiento. Ademas agrega un X-Correlation-Id (si no viene) que se
 * propaga a los microservicios; en la entrega 2 lo vamos a usar para el log centralizado.
 */
@Component
public class LogRequestFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID = "X-Correlation-Id";
    private static final Logger log = LoggerFactory.getLogger(LogRequestFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long inicio = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String id = correlationId;
        ServerHttpRequest conId = request.mutate().header(CORRELATION_ID, id).build();
        exchange.getResponse().getHeaders().set(CORRELATION_ID, id);

        return chain.filter(exchange.mutate().request(conId).build())
                .doFinally(signal -> {
                    String usuario = request.getHeaders().getFirst("X-Usuario-Id");
                    var status = exchange.getResponse().getStatusCode();
                    log.info("{} {} usuario={} status={} tiempo={}ms correlationId={}",
                            request.getMethod(), request.getURI().getRawPath(),
                            usuario == null ? "anonimo" : usuario,
                            status == null ? "-" : status.value(),
                            System.currentTimeMillis() - inicio, id);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
