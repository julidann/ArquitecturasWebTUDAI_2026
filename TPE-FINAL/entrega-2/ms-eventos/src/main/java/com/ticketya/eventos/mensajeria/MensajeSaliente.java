package com.ticketya.eventos.mensajeria;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Patron Transactional Outbox: el mensaje se guarda en la MISMA transaccion que el cambio de
 * negocio (ej: el evento pasa a PUBLICADO) y un job lo publica despues en RabbitMQ. Asi nunca queda
 * un cambio confirmado sin su mensaje (ni un mensaje de un cambio que hizo rollback), aunque RabbitMQ
 * este caido en ese momento.
 */
@Entity
@Table(name = "mensaje_saliente", indexes = @Index(name = "idx_mensaje_pendiente", columnList = "publicado_en"))
public class MensajeSaliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "routing_key", nullable = false, length = 60)
    private String routingKey;

    @Column(nullable = false, length = 20000)
    private String payload;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "publicado_en")
    private LocalDateTime publicadoEn;

    @Column(nullable = false)
    private int intentos;

    protected MensajeSaliente() {
    }

    public MensajeSaliente(String routingKey, String payload, String correlationId) {
        this.routingKey = routingKey;
        this.payload = payload;
        this.correlationId = correlationId;
    }

    public void marcarPublicado() {
        this.publicadoEn = LocalDateTime.now();
    }

    public void registrarIntento() {
        this.intentos++;
    }

    public Long getId() { return id; }
    public String getRoutingKey() { return routingKey; }
    public String getPayload() { return payload; }
    public String getCorrelationId() { return correlationId; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getPublicadoEn() { return publicadoEn; }
    public int getIntentos() { return intentos; }
}
