package com.ticketya.eventos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "asiento", uniqueConstraints = @UniqueConstraint(columnNames = {"sector_id", "fila", "numero"}))
public class Asiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id")
    private Sector sector;

    @Column(nullable = false, length = 5)
    private String fila;

    @Column(nullable = false)
    private Integer numero;

    protected Asiento() {
    }

    public Asiento(String fila, Integer numero) {
        this.fila = fila;
        this.numero = numero;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Sector getSector() { return sector; }
    void setSector(Sector sector) { this.sector = sector; }
    public String getFila() { return fila; }
    public Integer getNumero() { return numero; }
}
