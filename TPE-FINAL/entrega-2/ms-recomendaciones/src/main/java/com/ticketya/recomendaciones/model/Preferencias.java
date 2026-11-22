package com.ticketya.recomendaciones.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Preferencias de un cliente. Un documento por usuario (el _id es el id del usuario).
 * Ademas de los valores tal cual los cargo el cliente se guardan normalizados (minusculas, sin
 * espacios de mas) para poder buscar "a quien le interesa este artista en esta ciudad" con indices.
 */
@Document("preferencias")
public class Preferencias {

    @Id
    private Long usuarioId;
    private List<String> tiposEvento = new ArrayList<>();
    private List<String> artistasFavoritos = new ArrayList<>();
    private String ciudad;
    @Indexed
    private List<String> artistasNormalizados = new ArrayList<>();
    @Indexed
    private String ciudadNormalizada;
    private LocalDateTime actualizadas;

    protected Preferencias() {
    }

    public Preferencias(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public void actualizar(List<String> tipos, List<String> artistas, String ciudad, LocalDateTime ahora) {
        this.tiposEvento = tipos == null ? new ArrayList<>() : tipos.stream().map(t -> t.trim().toUpperCase(Locale.ROOT))
                .distinct().toList();
        this.artistasFavoritos = artistas == null ? new ArrayList<>() : artistas.stream().map(String::trim)
                .filter(a -> !a.isEmpty()).distinct().toList();
        this.artistasNormalizados = this.artistasFavoritos.stream().map(Preferencias::normalizar).distinct().toList();
        this.ciudad = ciudad == null || ciudad.isBlank() ? null : ciudad.trim();
        this.ciudadNormalizada = this.ciudad == null ? null : normalizar(this.ciudad);
        this.actualizadas = ahora;
    }

    public static String normalizar(String texto) {
        return texto == null ? null : texto.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public Long getUsuarioId() { return usuarioId; }
    public List<String> getTiposEvento() { return tiposEvento; }
    public List<String> getArtistasFavoritos() { return artistasFavoritos; }
    public String getCiudad() { return ciudad; }
    public List<String> getArtistasNormalizados() { return artistasNormalizados; }
    public String getCiudadNormalizada() { return ciudadNormalizada; }
    public LocalDateTime getActualizadas() { return actualizadas; }
}
