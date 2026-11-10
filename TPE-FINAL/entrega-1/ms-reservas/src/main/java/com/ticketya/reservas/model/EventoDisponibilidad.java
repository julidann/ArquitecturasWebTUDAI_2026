package com.ticketya.reservas.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Copia minima de los datos del evento que necesita este servicio para reservar sin
 * consultar a ms-eventos (entidad solapada con "evento" de ms-eventos).
 */
@Entity
@Table(name = "evento_disponibilidad")
public class EventoDisponibilidad {

    @Id
    @Column(name = "evento_id")
    private Long eventoId;

    @Column(name = "nombre_evento", nullable = false)
    private String nombreEvento;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento;

    @Column(name = "limite_por_cliente", nullable = false)
    private Integer limitePorCliente;

    @Column(nullable = false)
    private boolean habilitado = true;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    protected EventoDisponibilidad() {
    }

    public EventoDisponibilidad(Long eventoId, String nombreEvento, LocalDateTime fechaEvento, Integer limitePorCliente) {
        this.eventoId = eventoId;
        this.nombreEvento = nombreEvento;
        this.fechaEvento = fechaEvento;
        this.limitePorCliente = limitePorCliente;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
        this.actualizadoEn = LocalDateTime.now();
    }

    public Long getEventoId() { return eventoId; }
    public String getNombreEvento() { return nombreEvento; }
    public LocalDateTime getFechaEvento() { return fechaEvento; }
    public Integer getLimitePorCliente() { return limitePorCliente; }
    public boolean isHabilitado() { return habilitado; }
}
