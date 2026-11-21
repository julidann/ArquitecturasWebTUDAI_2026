package com.ticketya.notificaciones.trazabilidad;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/** Propaga el X-Correlation-Id del request actual a las llamadas REST hacia otros microservicios. */
public class CorrelacionInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String id = MDC.get(CorrelacionFilter.MDC_CLAVE);
        if (id != null && !request.getHeaders().containsKey(CorrelacionFilter.HEADER)) {
            request.getHeaders().add(CorrelacionFilter.HEADER, id);
        }
        return execution.execute(request, body);
    }
}
