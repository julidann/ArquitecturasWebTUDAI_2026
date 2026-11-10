package com.ticketya.reservas.model;

import jakarta.persistence.*;

/** Un lugar dentro de la reserva. Para sectores no numerados asientoId, fila y numero quedan en null. */
@Entity
@Table(name = "reserva_item")
public class ReservaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Column(name = "asiento_id")
    private Long asientoId;

    private String fila;
    private Integer numero;

    protected ReservaItem() {
    }

    public ReservaItem(Long asientoId, String fila, Integer numero) {
        this.asientoId = asientoId;
        this.fila = fila;
        this.numero = numero;
    }

    void setReserva(Reserva reserva) { this.reserva = reserva; }
    public Long getId() { return id; }
    public Long getAsientoId() { return asientoId; }
    public String getFila() { return fila; }
    public Integer getNumero() { return numero; }
}
