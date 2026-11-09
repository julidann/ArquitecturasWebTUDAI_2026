package com.ticketya.eventos.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Sector de un lugar. Si es NUMERADO tiene asientos identificados por fila y numero,
 * si es NO_NUMERADO solo tiene una capacidad maxima (ej: "Campo").
 */
@Entity
@Table(name = "sector")
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lugar_id")
    private Lugar lugar;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSector tipo;

    /** Para numerados se calcula como filas * asientos por fila. */
    @Column(nullable = false)
    private Integer capacidad;

    @OneToMany(mappedBy = "sector", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fila, numero")
    private List<Asiento> asientos = new ArrayList<>();

    protected Sector() {
    }

    public static Sector noNumerado(String nombre, int capacidad) {
        Sector s = new Sector();
        s.nombre = nombre;
        s.tipo = TipoSector.NO_NUMERADO;
        s.capacidad = capacidad;
        return s;
    }

    /**
     * Crea un sector numerado generando los asientos. Las filas se nombran con letras
     * (A, B, C...) y si hay mas de 26 se usa AA, AB, etc.
     */
    public static Sector numerado(String nombre, int filas, int asientosPorFila) {
        Sector s = new Sector();
        s.nombre = nombre;
        s.tipo = TipoSector.NUMERADO;
        for (int f = 0; f < filas; f++) {
            String fila = nombreFila(f);
            for (int n = 1; n <= asientosPorFila; n++) {
                Asiento a = new Asiento(fila, n);
                a.setSector(s);
                s.asientos.add(a);
            }
        }
        s.capacidad = filas * asientosPorFila;
        return s;
    }

    static String nombreFila(int indice) {
        StringBuilder sb = new StringBuilder();
        int i = indice;
        do {
            sb.insert(0, (char) ('A' + (i % 26)));
            i = i / 26 - 1;
        } while (i >= 0);
        return sb.toString();
    }

    public boolean esNumerado() {
        return tipo == TipoSector.NUMERADO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Lugar getLugar() { return lugar; }
    void setLugar(Lugar lugar) { this.lugar = lugar; }
    public String getNombre() { return nombre; }
    public TipoSector getTipo() { return tipo; }
    public Integer getCapacidad() { return capacidad; }
    public List<Asiento> getAsientos() { return asientos; }
}
