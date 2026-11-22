package com.ticketya.recomendaciones.service;

import com.ticketya.recomendaciones.dto.PreferenciasRequest;
import com.ticketya.recomendaciones.dto.PreferenciasResponse;
import com.ticketya.recomendaciones.model.Preferencias;
import com.ticketya.recomendaciones.repository.PreferenciasRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class PreferenciasService {

    private final PreferenciasRepository repository;
    private final Clock clock;

    public PreferenciasService(PreferenciasRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public PreferenciasResponse obtener(Long usuarioId) {
        return repository.findById(usuarioId).map(PreferenciasResponse::de)
                .orElse(PreferenciasResponse.vacias(usuarioId));
    }

    /** Reemplaza las preferencias completas (PUT): el documento entero se guarda de una vez. */
    public PreferenciasResponse guardar(Long usuarioId, PreferenciasRequest req) {
        Preferencias p = repository.findById(usuarioId).orElseGet(() -> new Preferencias(usuarioId));
        p.actualizar(req.tiposEvento(), req.artistasFavoritos(), req.ciudad(), LocalDateTime.now(clock));
        return PreferenciasResponse.de(repository.save(p));
    }

    /**
     * Clientes a los que les interesa un evento: tienen alguno de los participantes como artista
     * favorito y viven en la ciudad del evento. Lo usa ms-notificaciones (funcionalidad 11).
     */
    public List<Long> interesados(List<String> artistas, String ciudad) {
        if (artistas == null || artistas.isEmpty() || ciudad == null || ciudad.isBlank()) {
            return List.of();
        }
        List<String> normalizados = artistas.stream().filter(Objects::nonNull).map(Preferencias::normalizar).toList();
        return repository.interesados(normalizados, Preferencias.normalizar(ciudad)).stream()
                .map(Preferencias::getUsuarioId).sorted().toList();
    }
}
