package com.ticketya.eventos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "evento")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 2000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEvento tipo;

    /** Id del usuario organizador (los usuarios se gestionan en la entrega 2). */
    @Column(name = "organizador_id", nullable = false)
    private Long organizadorId;

    @ElementCollection
    @CollectionTable(name = "evento_participante", joinColumns = @JoinColumn(name = "evento_id"))
    @Column(name = "participante")
    private List<String> participantes = new ArrayList<>();

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lugar_id")
    private Lugar lugar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEvento estado = EstadoEvento.BORRADOR;

    /** Se usa en la entrega 2 para la fila virtual. */
    @Column(name = "alta_demanda", nullable = false)
    private boolean altaDemanda;

    @Column(name = "limite_entradas_cliente", nullable = false)
    private Integer limiteEntradasPorCliente = 6;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrecioSector> precios = new ArrayList<>();

    /** Desnormalizado para poder ordenar las busquedas por precio sin joins. */
    @Column(name = "precio_minimo", precision = 12, scale = 2)
    private BigDecimal precioMinimo;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    protected Evento() {
    }

    public Evento(String nombre, String descripcion, TipoEvento tipo, Long organizadorId,
                  List<String> participantes, LocalDateTime fechaHora, Lugar lugar) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.organizadorId = organizadorId;
        this.participantes = new ArrayList<>(participantes == null ? List.of() : participantes);
        this.fechaHora = fechaHora;
        this.lugar = lugar;
    }

    public void definirPrecio(Sector sector, BigDecimal precio) {
        precios.stream()
                .filter(p -> p.getSector().getId().equals(sector.getId()))
                .findFirst()
                .ifPresentOrElse(p -> p.setPrecio(precio),
                        () -> precios.add(new PrecioSector(this, sector, precio)));
        recalcularPrecioMinimo();
    }

    private void recalcularPrecioMinimo() {
        this.precioMinimo = precios.stream()
                .map(PrecioSector::getPrecio)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    public boolean tienePrecioParaTodosLosSectores() {
        return lugar.getSectores().stream()
                .allMatch(s -> precios.stream().anyMatch(p -> p.getSector().getId().equals(s.getId())));
    }

    public void cambiarEstado(EstadoEvento nuevo) {
        this.estado = nuevo;
        this.actualizadoEn = LocalDateTime.now();
    }

    public void actualizarDatos(String nombre, String descripcion, TipoEvento tipo, List<String> participantes,
                                LocalDateTime fechaHora, boolean altaDemanda, Integer limite) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.participantes.clear();
        if (participantes != null) {
            this.participantes.addAll(participantes);
        }
        this.fechaHora = fechaHora;
        this.altaDemanda = altaDemanda;
        if (limite != null) {
            this.limiteEntradasPorCliente = limite;
        }
        this.actualizadoEn = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public TipoEvento getTipo() { return tipo; }
    public Long getOrganizadorId() { return organizadorId; }
    public List<String> getParticipantes() { return participantes; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public Lugar getLugar() { return lugar; }
    public EstadoEvento getEstado() { return estado; }
    public boolean isAltaDemanda() { return altaDemanda; }
    public void setAltaDemanda(boolean altaDemanda) { this.altaDemanda = altaDemanda; }
    public Integer getLimiteEntradasPorCliente() { return limiteEntradasPorCliente; }
    public void setLimiteEntradasPorCliente(Integer limite) { this.limiteEntradasPorCliente = limite; }
    public List<PrecioSector> getPrecios() { return precios; }
    public BigDecimal getPrecioMinimo() { return precioMinimo; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
