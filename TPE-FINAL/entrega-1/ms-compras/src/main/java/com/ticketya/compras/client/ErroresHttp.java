package com.ticketya.compras.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketya.compras.exception.RespuestaServicioException;
import com.ticketya.compras.exception.ServicioNoDisponibleException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

/**
 * Traduce los errores de RestClient a excepciones propias:
 *  - 4xx -> RespuestaServicioException (con el mensaje que mando el otro servicio)
 *  - 5xx, timeouts, conexion rechazada -> ServicioNoDisponibleException
 */
final class ErroresHttp {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ErroresHttp() {
    }

    static <T> T llamar(String servicio, Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (HttpClientErrorException e) {
            throw new RespuestaServicioException(e.getStatusCode().value(), mensaje(e));
        } catch (RestClientResponseException e) {
            throw new ServicioNoDisponibleException(servicio + " respondió " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException(servicio + " no está disponible", e);
        }
    }

    private static String mensaje(RestClientResponseException e) {
        try {
            JsonNode json = MAPPER.readTree(e.getResponseBodyAsString());
            if (json.hasNonNull("mensaje")) {
                return json.get("mensaje").asText();
            }
        } catch (Exception ignored) {
            // el cuerpo no era el ErrorResponse esperado
        }
        return e.getStatusText();
    }
}
