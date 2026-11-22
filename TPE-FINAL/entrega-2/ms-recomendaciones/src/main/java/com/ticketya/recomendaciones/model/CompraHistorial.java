package com.ticketya.recomendaciones.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Historial de compras (una entrada por compra confirmada). El _id es el id de la compra, asi si el
 * mensaje compra.confirmada llega repetido (RabbitMQ entrega "al menos una vez") no se duplica.
 */
@Document("historial_compras")
@CompoundIndex(name = "usuario_evento", def = "{'usuarioId': 1, 'eventoId': 1}")
public class CompraHistorial {

    @Id
    private Long compraId;
    @Indexed
    private Long usuarioId;
    @Indexed
    private Long eventoId;
    private Integer cantidad;
    private LocalDateTime fecha;

    protected CompraHistorial() {
    }

    public CompraHistorial(Long compraId, Long usuarioId, Long eventoId, Integer cantidad, LocalDateTime fecha) {
        this.compraId = compraId;
        this.usuarioId = usuarioId;
        this.eventoId = eventoId;
        this.cantidad = cantidad;
        this.fecha = fecha;
    }

    public Long getCompraId() { return compraId; }
    public Long getUsuarioId() { return usuarioId; }
    public Long getEventoId() { return eventoId; }
    public Integer getCantidad() { return cantidad; }
    public LocalDateTime getFecha() { return fecha; }
}
