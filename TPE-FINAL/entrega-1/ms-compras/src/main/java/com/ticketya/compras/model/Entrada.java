package com.ticketya.compras.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "entrada", indexes = {
        @Index(name = "idx_entrada_usuario", columnList = "usuario_id"),
        @Index(name = "idx_entrada_evento", columnList = "evento_id")
})
public class Entrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id")
    private Compra compra;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Column(name = "asiento_id")
    private Long asientoId;

    private String fila;
    private Integer numero;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    /** Contenido del QR: un UUID aleatorio, imposible de adivinar a partir del id. */
    @Column(name = "codigo_qr", nullable = false, unique = true, length = 36)
    private String codigoQr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoEntrada estado = EstadoEntrada.VALIDA;

    /**
     * Ultima red de seguridad contra la doble venta: para asientos numerados vale
     * "eventoId-asientoId" mientras la entrada esta VALIDA (y null cuando se devuelve o
     * cancela). Al ser UNIQUE, la base rechaza dos entradas validas para el mismo asiento
     * aunque fallara todo lo demas.
     */
    @Column(name = "lugar_activo", unique = true, length = 50)
    private String lugarActivo;

    @Column(name = "emitida_en", nullable = false)
    private LocalDateTime emitidaEn = LocalDateTime.now();

    protected Entrada() {
    }

    public Entrada(Long usuarioId, Long eventoId, Long sectorId, Long asientoId, String fila, Integer numero,
                   BigDecimal precio) {
        this.usuarioId = usuarioId;
        this.eventoId = eventoId;
        this.sectorId = sectorId;
        this.asientoId = asientoId;
        this.fila = fila;
        this.numero = numero;
        this.precio = precio;
        this.codigoQr = UUID.randomUUID().toString();
        this.lugarActivo = asientoId == null ? null : eventoId + "-" + asientoId;
    }

    public void devolver() {
        this.estado = EstadoEntrada.DEVUELTA;
        this.lugarActivo = null;
    }

    public void cancelarPorEvento() {
        this.estado = EstadoEntrada.CANCELADA;
        this.lugarActivo = null;
    }

    void setCompra(Compra compra) { this.compra = compra; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Compra getCompra() { return compra; }
    public Long getUsuarioId() { return usuarioId; }
    public Long getEventoId() { return eventoId; }
    public Long getSectorId() { return sectorId; }
    public Long getAsientoId() { return asientoId; }
    public String getFila() { return fila; }
    public Integer getNumero() { return numero; }
    public BigDecimal getPrecio() { return precio; }
    public String getCodigoQr() { return codigoQr; }
    public EstadoEntrada getEstado() { return estado; }
    public String getLugarActivo() { return lugarActivo; }
    public LocalDateTime getEmitidaEn() { return emitidaEn; }
}
