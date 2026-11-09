package com.ticketya.eventos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "precio_sector", uniqueConstraints = @UniqueConstraint(columnNames = {"evento_id", "sector_id"}))
public class PrecioSector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id")
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id")
    private Sector sector;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    protected PrecioSector() {
    }

    public PrecioSector(Evento evento, Sector sector, BigDecimal precio) {
        this.evento = evento;
        this.sector = sector;
        this.precio = precio;
    }

    public Long getId() { return id; }
    public Evento getEvento() { return evento; }
    public Sector getSector() { return sector; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
}
