package com.ticketya.gateway.seguridad;

import com.ticketya.gateway.filtro.Headers;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reemplaza los headers de identidad que viajan a los microservicios por los datos del JWT ya
 * validado. Primero BORRA cualquier X-Usuario-Id / X-Rol que mande el cliente: si no, alguien
 * podria hacerse pasar por otro usuario o por administrador mandando el header a mano.
 */
@Component
public class IdentidadFilter implements GlobalFilter, Ordered {

    public static final String USUARIO = "X-Usuario-Id";
    public static final String ROL = "X-Rol";
    public static final String EMAIL = "X-Usuario-Email";
    /** Atributo del exchange que usa el log de requests para saber quien hizo el pedido. */
    public static final String ATRIBUTO_USUARIO = "ticketya.usuario";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(a -> a instanceof JwtAuthenticationToken)
                .map(a -> ((JwtAuthenticationToken) a).getToken())
                .map(jwt -> conIdentidad(exchange, jwt))
                .defaultIfEmpty(sinIdentidad(exchange))
                .flatMap(chain::filter);
    }

    private ServerWebExchange conIdentidad(ServerWebExchange exchange, Jwt jwt) {
        exchange.getAttributes().put(ATRIBUTO_USUARIO, jwt.getSubject() + "/" + jwt.getClaimAsString("rol"));
        ServerHttpRequest req = Headers.modificar(exchange.getRequest(), h -> {
            h.remove(USUARIO);
            h.remove(ROL);
            h.remove(EMAIL);
            h.set(USUARIO, jwt.getSubject());
            h.set(ROL, jwt.getClaimAsString("rol"));
            if (jwt.getClaimAsString("email") != null) {
                h.set(EMAIL, jwt.getClaimAsString("email"));
            }
        });
        return exchange.mutate().request(req).build();
    }

    private ServerWebExchange sinIdentidad(ServerWebExchange exchange) {
        ServerHttpRequest req = Headers.modificar(exchange.getRequest(), h -> {
            h.remove(USUARIO);
            h.remove(ROL);
            h.remove(EMAIL);
        });
        return exchange.mutate().request(req).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
