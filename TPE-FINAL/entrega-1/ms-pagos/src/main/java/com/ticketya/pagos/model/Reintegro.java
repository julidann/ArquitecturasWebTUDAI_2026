package com.ticketya.pagos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reintegro")
public class Reintegro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id")
    private Pago pago;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    private String motivo;

    /** En el mock los reintegros siempre se aprueban. */
    @Column(nullable = false)
    private String estado = "APROBADO";

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected Reintegro() {
    }

    public Reintegro(Pago pago, String idempotencyKey, BigDecimal monto, String motivo) {
        this.pago = pago;
        this.idempotencyKey = idempotencyKey;
        this.monto = monto;
        this.motivo = motivo;
    }

    public Long getId() { return id; }
    public Pago getPago() { return pago; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public BigDecimal getMonto() { return monto; }
    public String getMotivo() { return motivo; }
    public String getEstado() { return estado; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
