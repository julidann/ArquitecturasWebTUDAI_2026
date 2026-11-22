package com.ticketya.recomendaciones.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Copia local (read model) de los eventos publicados, armada con los mensajes de ms-eventos.
 * Asi las recomendaciones no dependen de que ms-eventos este disponible (consistencia eventual:
 * un evento recien publicado puede tardar unos milisegundos en aparecer aca).
 */
@Document("eventos")
public class EventoCatalogo {

    @Id
    private Long eventoId;
    private String nombre;
    private String tipo;
    private List<String> participantes;
    @Indexed
    private List<String> participantesNormalizados;
    private String lugar;
    private String ciudad;
    @Indexed
    private String ciudadNormalizada;
    private LocalDateTime fechaHora;
    private BigDecimal precioMinimo;
    private boolean cancelado;

    protected EventoCatalogo() {
    }

    public EventoCatalogo(Long eventoId, String nombre, String tipo, List<String> participantes, String lugar,
                          String ciudad, LocalDateTime fechaHora, BigDecimal precioMinimo) {
        this.eventoId = eventoId;
        this.nombre = nombre;
        this.tipo = tipo;
        this.participantes = participantes == null ? List.of() : participantes;
        this.participantesNormalizados = this.participantes.stream().map(Preferencias::normalizar).toList();
        this.lugar = lugar;
        this.ciudad = ciudad;
        this.ciudadNormalizada = Preferencias.normalizar(ciudad);
        this.fechaHora = fechaHora;
        this.precioMinimo = precioMinimo;
    }

    public void cancelar() {
        this.cancelado = true;
    }

    public void reprogramar(LocalDateTime nuevaFecha) {
        this.fechaHora = nuevaFecha;
    }

    public boolean vigente(LocalDateTime ahora) {
        return !cancelado && fechaHora != null && fechaHora.isAfter(ahora);
    }

    public Long getEventoId() { return eventoId; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public List<String> getParticipantes() { return participantes; }
    public List<String> getParticipantesNormalizados() { return participantesNormalizados; }
    public String getLugar() { return lugar; }
    public String getCiudad() { return ciudad; }
    public String getCiudadNormalizada() { return ciudadNormalizada; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public BigDecimal getPrecioMinimo() { return precioMinimo; }
    public boolean isCancelado() { return cancelado; }
}
