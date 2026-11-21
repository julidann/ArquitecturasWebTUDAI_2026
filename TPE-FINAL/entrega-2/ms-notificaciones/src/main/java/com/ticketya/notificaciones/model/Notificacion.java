package com.ticketya.notificaciones.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Una notificacion para un usuario. La "clave" (tipo + referencia + usuario) tiene indice unico:
 * si el mismo mensaje de RabbitMQ llega dos veces, la segunda insercion falla y no se manda un
 * email repetido (idempotencia del consumidor).
 */
@Document("notificaciones")
@CompoundIndex(name = "pendientes", def = "{'estado': 1, 'proximoIntento': 1}")
public class Notificacion {

    @Id
    private String id;
    @Indexed(unique = true)
    private String clave;
    @Indexed
    private Long usuarioId;
    private TipoNotificacion tipo;
    private String canal = "EMAIL";
    private Long eventoId;
    private String asunto;
    private String mensaje;
    private String destino;
    private EstadoNotificacion estado = EstadoNotificacion.PENDIENTE;
    private int intentos;
    private String ultimoError;
    private String correlationId;
    private LocalDateTime creada;
    private LocalDateTime enviada;
    private LocalDateTime proximoIntento;
    @Version
    private Long version;

    protected Notificacion() {
    }

    public Notificacion(TipoNotificacion tipo, String referencia, Long usuarioId, Long eventoId, String asunto,
                        String mensaje, String correlationId, LocalDateTime ahora) {
        this.clave = tipo + ":" + referencia + ":" + usuarioId;
        this.tipo = tipo;
        this.usuarioId = usuarioId;
        this.eventoId = eventoId;
        this.asunto = asunto;
        this.mensaje = mensaje;
        this.correlationId = correlationId;
        this.creada = ahora;
        this.proximoIntento = ahora;
    }

    public void marcarEnviada(String destino, LocalDateTime ahora) {
        this.destino = destino;
        this.estado = EstadoNotificacion.ENVIADA;
        this.intentos++;
        this.enviada = ahora;
        this.ultimoError = null;
        this.proximoIntento = null;
    }

    /** Fallo el envio: se reintenta mas tarde con espera creciente, o queda FALLIDA si no quedan intentos. */
    public void registrarFallo(String error, int maxIntentos, LocalDateTime ahora) {
        this.intentos++;
        this.ultimoError = error;
        if (intentos >= maxIntentos) {
            this.estado = EstadoNotificacion.FALLIDA;
            this.proximoIntento = null;
        } else {
            this.proximoIntento = ahora.plusSeconds(30L * intentos);
        }
    }

    public void descartar(String motivo) {
        this.estado = EstadoNotificacion.DESCARTADA;
        this.ultimoError = motivo;
        this.proximoIntento = null;
    }

    public String getId() { return id; }
    public String getClave() { return clave; }
    public Long getUsuarioId() { return usuarioId; }
    public TipoNotificacion getTipo() { return tipo; }
    public String getCanal() { return canal; }
    public Long getEventoId() { return eventoId; }
    public String getAsunto() { return asunto; }
    public String getMensaje() { return mensaje; }
    public String getDestino() { return destino; }
    public EstadoNotificacion getEstado() { return estado; }
    public int getIntentos() { return intentos; }
    public String getUltimoError() { return ultimoError; }
    public String getCorrelationId() { return correlationId; }
    public LocalDateTime getCreada() { return creada; }
    public LocalDateTime getEnviada() { return enviada; }
    public LocalDateTime getProximoIntento() { return proximoIntento; }
}
