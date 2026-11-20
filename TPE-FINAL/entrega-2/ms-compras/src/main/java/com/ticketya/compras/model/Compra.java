package com.ticketya.compras.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compra", indexes = @Index(name = "idx_compra_usuario", columnList = "usuario_id"))
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** La manda el cliente; si reintenta con la misma clave no se procesa dos veces. */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /** Una reserva solo puede tener una compra: evita cobrar dos veces los mismos lugares. */
    @Column(name = "reserva_id", nullable = false, unique = true)
    private Long reservaId;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "nombre_evento")
    private String nombreEvento;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Column(name = "sector_nombre")
    private String sectorNombre;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false)
    private MedioPago medioPago;

    @Column(name = "ultimos_digitos", length = 4)
    private String ultimosDigitos;

    @Column(name = "pago_id")
    private Long pagoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoCompra estado = EstadoCompra.PENDIENTE;

    private String detalle;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    @Column(name = "actualizada_en")
    private LocalDateTime actualizadaEn;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Entrada> entradas = new ArrayList<>();

    protected Compra() {
    }

    public Compra(String idempotencyKey, Long usuarioId, Long reservaId, Long eventoId, String nombreEvento,
                  LocalDateTime fechaEvento, Long sectorId, String sectorNombre, Integer cantidad,
                  BigDecimal montoTotal, MedioPago medioPago, String ultimosDigitos) {
        this.idempotencyKey = idempotencyKey;
        this.usuarioId = usuarioId;
        this.reservaId = reservaId;
        this.eventoId = eventoId;
        this.nombreEvento = nombreEvento;
        this.fechaEvento = fechaEvento;
        this.sectorId = sectorId;
        this.sectorNombre = sectorNombre;
        this.cantidad = cantidad;
        this.montoTotal = montoTotal;
        this.medioPago = medioPago;
        this.ultimosDigitos = ultimosDigitos;
    }

    public void agregarEntrada(Entrada entrada) {
        entrada.setCompra(this);
        entradas.add(entrada);
    }

    public void aprobar(Long pagoId) {
        this.pagoId = pagoId;
        cambiar(EstadoCompra.APROBADA, null);
    }

    public void rechazar(Long pagoId, String motivo) {
        this.pagoId = pagoId;
        cambiar(EstadoCompra.RECHAZADA, motivo);
    }

    public void marcarError(String motivo) {
        cambiar(EstadoCompra.ERROR, motivo);
    }

    public void registrarPago(Long pagoId) {
        this.pagoId = pagoId;
    }

    private void cambiar(EstadoCompra nuevo, String detalle) {
        this.estado = nuevo;
        this.detalle = detalle;
        this.actualizadaEn = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getUsuarioId() { return usuarioId; }
    public Long getReservaId() { return reservaId; }
    public Long getEventoId() { return eventoId; }
    public String getNombreEvento() { return nombreEvento; }
    public LocalDateTime getFechaEvento() { return fechaEvento; }
    public Long getSectorId() { return sectorId; }
    public String getSectorNombre() { return sectorNombre; }
    public Integer getCantidad() { return cantidad; }
    public BigDecimal getMontoTotal() { return montoTotal; }
    public MedioPago getMedioPago() { return medioPago; }
    public String getUltimosDigitos() { return ultimosDigitos; }
    public Long getPagoId() { return pagoId; }
    public EstadoCompra getEstado() { return estado; }
    public String getDetalle() { return detalle; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public LocalDateTime getActualizadaEn() { return actualizadaEn; }
    public List<Entrada> getEntradas() { return entradas; }
}
