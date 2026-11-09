package com.ticketya.compras.client;

import com.ticketya.compras.dto.DatosMedioPago;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class PagosRestClient implements PagosClient {

    private static final String SERVICIO = "ms-pagos";
    private final RestClient restClient;

    public PagosRestClient(@Qualifier("pagosHttp") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public PagoDto pagar(Long compraId, Long usuarioId, BigDecimal monto, DatosMedioPago medioPago,
                         String idempotencyKey) {
        Map<String, Object> body = Map.of(
                "compraId", compraId,
                "usuarioId", usuarioId,
                "monto", monto,
                "medioPago", medioPago);
        return ErroresHttp.llamar(SERVICIO, () -> restClient.post()
                .uri("/pagos")
                .header("Idempotency-Key", idempotencyKey)
                .body(body)
                .retrieve()
                .body(PagoDto.class));
    }

    @Override
    public ReintegroDto reintegrar(Long pagoId, BigDecimal monto, String motivo, String idempotencyKey) {
        return ErroresHttp.llamar(SERVICIO, () -> restClient.post()
                .uri("/pagos/{id}/reintegros", pagoId)
                .header("Idempotency-Key", idempotencyKey)
                .body(Map.of("monto", monto, "motivo", motivo == null ? "" : motivo))
                .retrieve()
                .body(ReintegroDto.class));
    }
}
