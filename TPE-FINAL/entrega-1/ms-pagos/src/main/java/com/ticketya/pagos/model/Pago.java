package com.ticketya.pagos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pago", indexes = @Index(name = "idx_pago_compra", columnList = "compra_id"))
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Clave de idempotencia enviada por ms-compras: un reintento no cobra dos veces. */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "compra_id", nullable = false)
    private Long compraId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false)
    private MedioPago medioPago;

    /** Nunca se guarda el numero completo de la tarjeta, solo los ultimos 4 digitos. */
    @Column(name = "ultimos_digitos", length = 4)
    private String ultimosDigitos;

    private String titular;

    private Integer cuotas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPago estado;

    @Column(name = "codigo_autorizacion")
    private String codigoAutorizacion;

    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Reintegro> reintegros = new ArrayList<>();

    protected Pago() {
    }

    public Pago(String idempotencyKey, Long compraId, Long usuarioId, BigDecimal monto, MedioPago medioPago,
                String ultimosDigitos, String titular, Integer cuotas) {
        this.idempotencyKey = idempotencyKey;
        this.compraId = compraId;
        this.usuarioId = usuarioId;
        this.monto = monto;
        this.medioPago = medioPago;
        this.ultimosDigitos = ultimosDigitos;
        this.titular = titular;
        this.cuotas = cuotas;
    }

    public void aprobar(String codigoAutorizacion) {
        this.estado = EstadoPago.APROBADO;
        this.codigoAutorizacion = codigoAutorizacion;
    }

    public void rechazar(String motivo) {
        this.estado = EstadoPago.RECHAZADO;
        this.motivoRechazo = motivo;
    }

    public BigDecimal totalReintegrado() {
        return reintegros.stream().map(Reintegro::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getCompraId() { return compraId; }
    public Long getUsuarioId() { return usuarioId; }
    public BigDecimal getMonto() { return monto; }
    public MedioPago getMedioPago() { return medioPago; }
    public String getUltimosDigitos() { return ultimosDigitos; }
    public String getTitular() { return titular; }
    public Integer getCuotas() { return cuotas; }
    public EstadoPago getEstado() { return estado; }
    public String getCodigoAutorizacion() { return codigoAutorizacion; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public List<Reintegro> getReintegros() { return reintegros; }
}
