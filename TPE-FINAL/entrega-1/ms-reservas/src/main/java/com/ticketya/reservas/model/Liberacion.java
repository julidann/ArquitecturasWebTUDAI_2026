package com.ticketya.reservas.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Registro de lugares vendidos que se volvieron a liberar por una devolucion. La clave
 * es unica (la manda ms-compras como Idempotency-Key) para que un reintento no libere
 * dos veces el mismo lugar y "agrande" la capacidad de una zona.
 */
@Entity
@Table(name = "liberacion")
public class Liberacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String clave;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Column(name = "asiento_id")
    private Long asientoId;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    protected Liberacion() {
    }

    public Liberacion(String clave, Long eventoId, Long reservaId, Long sectorId, Long asientoId, Integer cantidad) {
        this.clave = clave;
        this.eventoId = eventoId;
        this.reservaId = reservaId;
        this.sectorId = sectorId;
        this.asientoId = asientoId;
        this.cantidad = cantidad;
    }

    public Long getId() { return id; }
    public String getClave() { return clave; }
}
