package com.ticketya.eventos.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lugar")
public class Lugar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String direccion;

    @Column(nullable = false)
    private String ciudad;

    private Double latitud;
    private Double longitud;

    @OneToMany(mappedBy = "lugar", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Sector> sectores = new ArrayList<>();

    protected Lugar() {
    }

    public Lugar(String nombre, String direccion, String ciudad, Double latitud, Double longitud) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.ciudad = ciudad;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public void agregarSector(Sector sector) {
        sector.setLugar(this);
        sectores.add(sector);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }
    public String getCiudad() { return ciudad; }
    public Double getLatitud() { return latitud; }
    public Double getLongitud() { return longitud; }
    public List<Sector> getSectores() { return sectores; }
}
