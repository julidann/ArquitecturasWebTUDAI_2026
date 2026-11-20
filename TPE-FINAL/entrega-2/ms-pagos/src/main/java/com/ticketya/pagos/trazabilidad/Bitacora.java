package com.ticketya.pagos.trazabilidad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Registra las operaciones criticas (compra, devolucion, cancelacion, ingreso, ...) en el log
 * centralizado: se publican en el exchange "ticketya.bitacora" de RabbitMQ y ms-bitacora las guarda
 * en MongoDB, donde se pueden consultar por correlationId para seguir una operacion entre servicios.
 *
 * Es fire-and-forget: si RabbitMQ no esta disponible la operacion de negocio NO falla; el registro
 * queda igual en el log local del servicio (con el mismo correlationId).
 */
@Component
public class Bitacora {

    public static final String EXCHANGE = "ticketya.bitacora";
    private static final Logger log = LoggerFactory.getLogger("bitacora.operaciones");

    private final AmqpTemplate amqp;
    private final String servicio;
    private final boolean enviar;

    public Bitacora(AmqpTemplate amqp, @Value("${spring.application.name:ms}") String servicio,
                    @Value("${bitacora.enviar:true}") boolean enviar) {
        this.amqp = amqp;
        this.servicio = servicio;
        this.enviar = enviar;
    }

    public void registrar(String operacion, String resultado, Long usuarioId, Map<String, Object> detalle) {
        RegistroBitacora registro = new RegistroBitacora(CorrelacionFilter.actual(), servicio, operacion, resultado,
                usuarioId, detalle == null ? Map.of() : detalle, LocalDateTime.now());
        log.info("operacion={} resultado={} usuario={} detalle={}", operacion, resultado, usuarioId, registro.detalle());
        if (!enviar) {
            return;
        }
        try {
            amqp.convertAndSend(EXCHANGE, operacion, registro);
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el registro de bitacora a RabbitMQ ({}); queda solo en el log local",
                    e.getMessage());
        }
    }

    public record RegistroBitacora(String correlationId, String servicio, String operacion, String resultado,
                                   Long usuarioId, Map<String, Object> detalle, LocalDateTime fecha) {
    }
}
