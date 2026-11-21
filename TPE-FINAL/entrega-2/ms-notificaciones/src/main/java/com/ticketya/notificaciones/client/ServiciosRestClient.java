package com.ticketya.notificaciones.client;

import com.ticketya.notificaciones.exception.ServicioNoDisponibleException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Llamadas a ms-usuarios, ms-compras y ms-recomendaciones con timeout + reintentos (Resilience4j).
 * Si despues de los reintentos siguen sin responder se lanza ServicioNoDisponibleException: el
 * listener la deja pasar y RabbitMQ reintenta el mensaje (y si sigue fallando va a la DLQ).
 */
@Component
public class ServiciosRestClient implements ServiciosClient {

    private static final ParameterizedTypeReference<List<Long>> IDS = new ParameterizedTypeReference<>() {
    };

    private final RestClient usuarios;
    private final RestClient compras;
    private final RestClient recomendaciones;

    public ServiciosRestClient(@Qualifier("usuariosHttp") RestClient usuarios,
                               @Qualifier("comprasHttp") RestClient compras,
                               @Qualifier("recomendacionesHttp") RestClient recomendaciones) {
        this.usuarios = usuarios;
        this.compras = compras;
        this.recomendaciones = recomendaciones;
    }

    @Override
    @Retry(name = "servicios")
    public Optional<ContactoDto> contacto(Long usuarioId) {
        try {
            return Optional.ofNullable(llamar("ms-usuarios", () -> usuarios.get()
                    .uri("/internal/usuarios/{id}", usuarioId).retrieve().body(ContactoDto.class)));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    @Override
    @Retry(name = "servicios")
    public List<Long> compradores(Long eventoId) {
        return llamar("ms-compras", () -> compras.get()
                .uri("/internal/eventos/{id}/compradores", eventoId).retrieve().body(IDS));
    }

    @Override
    @Retry(name = "servicios")
    @CircuitBreaker(name = "recomendaciones")
    public List<Long> interesados(List<String> artistas, String ciudad) {
        return llamar("ms-recomendaciones", () -> recomendaciones.get()
                .uri(u -> u.path("/internal/preferencias/interesados").queryParam("artistas", artistas.toArray())
                        .queryParam("ciudad", ciudad).build())
                .retrieve().body(IDS));
    }

    private static <T> T llamar(String servicio, Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException(servicio + " no está disponible", e);
        }
    }
}
