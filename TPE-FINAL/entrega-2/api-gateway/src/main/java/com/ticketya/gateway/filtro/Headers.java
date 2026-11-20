package com.ticketya.gateway.filtro;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;

import java.util.function.Consumer;

/**
 * Devuelve el request con una copia modificable de los headers. (Los headers que llegan despues de
 * Spring Security son de solo lectura y request.mutate().headers(...) falla al borrar.)
 */
public final class Headers {

    private Headers() {
    }

    public static ServerHttpRequest modificar(ServerHttpRequest request, Consumer<HttpHeaders> cambios) {
        HttpHeaders copia = new HttpHeaders();
        copia.putAll(request.getHeaders());
        cambios.accept(copia);
        HttpHeaders soloLectura = HttpHeaders.readOnlyHttpHeaders(copia);
        return new ServerHttpRequestDecorator(request) {
            @Override
            public HttpHeaders getHeaders() {
                return soloLectura;
            }
        };
    }
}
