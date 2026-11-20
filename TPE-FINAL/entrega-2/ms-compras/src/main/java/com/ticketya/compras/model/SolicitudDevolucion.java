package com.ticketya.compras.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud_devolucion")
public class SolicitudDevolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "entrada_id")
    private Entrada entrada;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(name = "solicitada_en", nullable = false)
    private LocalDateTime solicitadaEn;

    @Column(name = "resuelta_en")
    private LocalDateTime resueltaEn;

    /** Id del administrador que la resolvio. */
    @Column(name = "resuelta_por")
    private Long resueltaPor;

    private String observacion;

    @Column(name = "reintegro_id")
    private Long reintegroId;

    protected SolicitudDevolucion() {
    }

    public SolicitudDevolucion(Entrada entrada, Long usuarioId, String motivo, LocalDateTime ahora) {
        this.entrada = entrada;
        this.usuarioId = usuarioId;
        this.motivo = motivo;
        this.solicitadaEn = ahora;
    }

    public void aprobar(Long adminId, Long reintegroId, LocalDateTime ahora) {
        this.estado = EstadoSolicitud.APROBADA;
        this.resueltaPor = adminId;
        this.reintegroId = reintegroId;
        this.resueltaEn = ahora;
    }

    public void rechazar(Long adminId, String observacion, LocalDateTime ahora) {
        this.estado = EstadoSolicitud.RECHAZADA;
        this.resueltaPor = adminId;
        this.observacion = observacion;
        this.resueltaEn = ahora;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Entrada getEntrada() { return entrada; }
    public Long getUsuarioId() { return usuarioId; }
    public String getMotivo() { return motivo; }
    public EstadoSolicitud getEstado() { return estado; }
    public LocalDateTime getSolicitadaEn() { return solicitadaEn; }
    public LocalDateTime getResueltaEn() { return resueltaEn; }
    public Long getResueltaPor() { return resueltaPor; }
    public String getObservacion() { return observacion; }
    public Long getReintegroId() { return reintegroId; }
}
