package com.ticketya.reservas.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reserva", indexes = {
        @Index(name = "idx_reserva_estado_expira", columnList = "estado, expira_en"),
        @Index(name = "idx_reserva_usuario_evento", columnList = "usuario_id, evento_id")
})
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_sector", nullable = false)
    private TipoSector tipoSector;

    @Column(nullable = false)
    private Integer cantidad;

    /** Lugares de esta reserva que despues fueron devueltos (no cuentan para el limite por cliente). */
    @Column(name = "cantidad_devuelta", nullable = false)
    private Integer cantidadDevuelta = 0;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoReserva estado = EstadoReserva.PENDIENTE;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "finalizada_en")
    private LocalDateTime finalizadaEn;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservaItem> items = new ArrayList<>();

    protected Reserva() {
    }

    public Reserva(Long usuarioId, Long eventoId, SectorDisponibilidad sector, int cantidad,
                   LocalDateTime ahora, int minutosVigencia) {
        this.usuarioId = usuarioId;
        this.eventoId = eventoId;
        this.sectorId = sector.getSectorId();
        this.tipoSector = sector.getTipo();
        this.cantidad = cantidad;
        this.precioUnitario = sector.getPrecio();
        this.montoTotal = sector.getPrecio().multiply(BigDecimal.valueOf(cantidad));
        this.creadaEn = ahora;
        this.expiraEn = ahora.plusMinutes(minutosVigencia);
    }

    public void agregarItem(ReservaItem item) {
        item.setReserva(this);
        items.add(item);
    }

    public boolean estaVencida(LocalDateTime ahora) {
        return estado == EstadoReserva.PENDIENTE && !ahora.isBefore(expiraEn);
    }

    public void registrarDevolucion(int cant) {
        this.cantidadDevuelta = Math.min(cantidad, cantidadDevuelta + cant);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public Long getEventoId() { return eventoId; }
    public Long getSectorId() { return sectorId; }
    public TipoSector getTipoSector() { return tipoSector; }
    public Integer getCantidad() { return cantidad; }
    public Integer getCantidadDevuelta() { return cantidadDevuelta; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getMontoTotal() { return montoTotal; }
    public EstadoReserva getEstado() { return estado; }
    public void setEstado(EstadoReserva estado) { this.estado = estado; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public LocalDateTime getExpiraEn() { return expiraEn; }
    public LocalDateTime getFinalizadaEn() { return finalizadaEn; }
    public List<ReservaItem> getItems() { return items; }
}
