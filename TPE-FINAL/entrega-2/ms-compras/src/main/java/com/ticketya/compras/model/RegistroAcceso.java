package com.ticketya.compras.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Cada escaneo en el ingreso (permitido o rechazado) queda registrado para auditoria. */
@Entity
@Table(name = "registro_acceso", indexes = @Index(name = "idx_acceso_evento", columnList = "evento_id"))
public class RegistroAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "codigo_qr", nullable = false, length = 100)
    private String codigoQr;

    @Column(name = "entrada_id")
    private Long entradaId;

    @Column(name = "personal_id")
    private Long personalId;

    @Column(length = 60)
    private String puerta;

    @Column(nullable = false, length = 30)
    private String resultado;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    protected RegistroAcceso() {
    }

    public RegistroAcceso(Long eventoId, String codigoQr, Long entradaId, Long personalId, String puerta,
                          String resultado, LocalDateTime fecha) {
        this.eventoId = eventoId;
        this.codigoQr = codigoQr;
        this.entradaId = entradaId;
        this.personalId = personalId;
        this.puerta = puerta;
        this.resultado = resultado;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public Long getEventoId() { return eventoId; }
    public String getCodigoQr() { return codigoQr; }
    public Long getEntradaId() { return entradaId; }
    public Long getPersonalId() { return personalId; }
    public String getPuerta() { return puerta; }
    public String getResultado() { return resultado; }
    public LocalDateTime getFecha() { return fecha; }
}
