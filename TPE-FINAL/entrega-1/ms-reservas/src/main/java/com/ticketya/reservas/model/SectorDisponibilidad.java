package com.ticketya.reservas.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Cupo de un sector para un evento. En los sectores no numerados el campo "disponibles"
 * es el que se descuenta en forma atomica para evitar la sobreventa; en los numerados se
 * mantiene como contador (el control real se hace por asiento).
 */
@Entity
@Table(name = "sector_disponibilidad",
        uniqueConstraints = @UniqueConstraint(columnNames = {"evento_id", "sector_id"}))
public class SectorDisponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSector tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer capacidad;

    @Column(nullable = false)
    private Integer disponibles;

    @Version
    private Long version;

    protected SectorDisponibilidad() {
    }

    public SectorDisponibilidad(Long eventoId, Long sectorId, String nombre, TipoSector tipo, BigDecimal precio,
                                Integer capacidad) {
        this.eventoId = eventoId;
        this.sectorId = sectorId;
        this.nombre = nombre;
        this.tipo = tipo;
        this.precio = precio;
        this.capacidad = capacidad;
        this.disponibles = capacidad;
    }

    public Long getId() { return id; }
    public Long getEventoId() { return eventoId; }
    public Long getSectorId() { return sectorId; }
    public String getNombre() { return nombre; }
    public TipoSector getTipo() { return tipo; }
    public BigDecimal getPrecio() { return precio; }
    public Integer getCapacidad() { return capacidad; }
    public Integer getDisponibles() { return disponibles; }
    public Long getVersion() { return version; }
}
