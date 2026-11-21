package com.ticketya.bitacora.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Una operacion critica registrada por algun microservicio. El "detalle" es libre (cada operacion
 * manda datos distintos), por eso se guarda en MongoDB como subdocumento sin esquema fijo.
 */
@Document("registros")
@CompoundIndex(name = "operacion_fecha", def = "{'operacion': 1, 'fecha': -1}")
public class Registro {

    @Id
    private String id;
    @Indexed
    private String correlationId;
    private String servicio;
    private String operacion;
    private String resultado;
    @Indexed
    private Long usuarioId;
    private Map<String, Object> detalle;
    @Indexed
    private LocalDateTime fecha;
    private LocalDateTime recibido;

    protected Registro() {
    }

    public Registro(String correlationId, String servicio, String operacion, String resultado, Long usuarioId,
                    Map<String, Object> detalle, LocalDateTime fecha, LocalDateTime recibido) {
        this.correlationId = correlationId;
        this.servicio = servicio;
        this.operacion = operacion;
        this.resultado = resultado;
        this.usuarioId = usuarioId;
        this.detalle = detalle;
        this.fecha = fecha;
        this.recibido = recibido;
    }

    public String getId() { return id; }
    public String getCorrelationId() { return correlationId; }
    public String getServicio() { return servicio; }
    public String getOperacion() { return operacion; }
    public String getResultado() { return resultado; }
    public Long getUsuarioId() { return usuarioId; }
    public Map<String, Object> getDetalle() { return detalle; }
    public LocalDateTime getFecha() { return fecha; }
    public LocalDateTime getRecibido() { return recibido; }
}
