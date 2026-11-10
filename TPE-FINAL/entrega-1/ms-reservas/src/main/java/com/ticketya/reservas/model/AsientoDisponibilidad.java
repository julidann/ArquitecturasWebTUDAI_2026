package com.ticketya.reservas.model;

import jakarta.persistence.*;

/**
 * Estado de un asiento numerado para un evento puntual. La restriccion de unicidad
 * (evento_id, asiento_id) garantiza que exista una sola fila por asiento y evento, y
 * el estado solo se modifica con UPDATEs condicionales (ver AsientoDisponibilidadRepository).
 */
@Entity
@Table(name = "asiento_disponibilidad",
        uniqueConstraints = @UniqueConstraint(columnNames = {"evento_id", "asiento_id"}),
        indexes = {
                @Index(name = "idx_asiento_evento_sector", columnList = "evento_id, sector_id"),
                @Index(name = "idx_asiento_reserva", columnList = "reserva_id")
        })
public class AsientoDisponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Column(name = "asiento_id", nullable = false)
    private Long asientoId;

    @Column(nullable = false, length = 5)
    private String fila;

    @Column(nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoAsiento estado = EstadoAsiento.DISPONIBLE;

    @Column(name = "reserva_id")
    private Long reservaId;

    @Version
    private Long version;

    protected AsientoDisponibilidad() {
    }

    public AsientoDisponibilidad(Long eventoId, Long sectorId, Long asientoId, String fila, Integer numero) {
        this.eventoId = eventoId;
        this.sectorId = sectorId;
        this.asientoId = asientoId;
        this.fila = fila;
        this.numero = numero;
    }

    public Long getId() { return id; }
    public Long getEventoId() { return eventoId; }
    public Long getSectorId() { return sectorId; }
    public Long getAsientoId() { return asientoId; }
    public String getFila() { return fila; }
    public Integer getNumero() { return numero; }
    public EstadoAsiento getEstado() { return estado; }
    public Long getReservaId() { return reservaId; }
}
