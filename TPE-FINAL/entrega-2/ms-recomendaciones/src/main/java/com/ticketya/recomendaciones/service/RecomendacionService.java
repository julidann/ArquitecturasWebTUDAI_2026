package com.ticketya.recomendaciones.service;

import com.ticketya.recomendaciones.dto.RecomendacionResponse;
import com.ticketya.recomendaciones.dto.TambienCompraronResponse;
import com.ticketya.recomendaciones.model.CompraHistorial;
import com.ticketya.recomendaciones.model.EventoCatalogo;
import com.ticketya.recomendaciones.model.Preferencias;
import com.ticketya.recomendaciones.repository.CompraHistorialRepository;
import com.ticketya.recomendaciones.repository.EventoCatalogoRepository;
import com.ticketya.recomendaciones.repository.PreferenciasRepository;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

/**
 * Recomendaciones de eventos (funcionalidad 10). Se suman puntos por cada motivo:
 *  - +5 si participa un artista favorito del cliente
 *  - +3 por cada cliente que compro lo mismo que el y tambien compro este evento (co-compra)
 *  - +2 si es de un tipo que le gusta (preferencias o historial) y es en su ciudad
 *  - +1 si es de un tipo que le gusta, en otra ciudad
 * Solo se sugieren eventos vigentes (no cancelados ni pasados) que el cliente todavia no compro.
 */
@Service
public class RecomendacionService {

    static final int PUNTOS_ARTISTA = 5;
    static final int PUNTOS_COCOMPRA = 3;
    static final int PUNTOS_TIPO_EN_CIUDAD = 2;
    static final int PUNTOS_TIPO = 1;

    private final MongoTemplate mongo;
    private final PreferenciasRepository preferencias;
    private final EventoCatalogoRepository eventos;
    private final CompraHistorialRepository historial;
    private final Clock clock;

    public RecomendacionService(MongoTemplate mongo, PreferenciasRepository preferencias,
                                EventoCatalogoRepository eventos, CompraHistorialRepository historial, Clock clock) {
        this.mongo = mongo;
        this.preferencias = preferencias;
        this.eventos = eventos;
        this.historial = historial;
        this.clock = clock;
    }

    public List<RecomendacionResponse> recomendar(Long usuarioId, int limite) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        Optional<Preferencias> pref = preferencias.findById(usuarioId);
        List<CompraHistorial> compras = historial.findByUsuarioId(usuarioId);
        Set<Long> comprados = new HashSet<>();
        compras.forEach(c -> comprados.add(c.getEventoId()));

        Map<Long, Candidato> candidatos = new HashMap<>();

        // 1) artistas favoritos
        List<String> artistas = pref.map(Preferencias::getArtistasNormalizados).orElse(List.of());
        if (!artistas.isEmpty()) {
            Query q = Query.query(Criteria.where("participantesNormalizados").in(artistas).and("cancelado").is(false)
                    .and("fechaHora").gt(ahora));
            for (EventoCatalogo e : mongo.find(q, EventoCatalogo.class)) {
                String artista = e.getParticipantes().stream()
                        .filter(p -> artistas.contains(Preferencias.normalizar(p))).findFirst().orElse("");
                candidatos.computeIfAbsent(e.getEventoId(), id -> new Candidato(e))
                        .sumar(PUNTOS_ARTISTA, "Participa " + artista + ", uno de tus artistas favoritos");
            }
        }

        // 2) co-compra: eventos que compraron otros clientes que compraron lo mismo que este cliente
        if (!comprados.isEmpty()) {
            for (Map.Entry<Long, Long> cc : coCompras(comprados, usuarioId).entrySet()) {
                eventos.findById(cc.getKey()).filter(e -> e.vigente(ahora)).ifPresent(e ->
                        candidatos.computeIfAbsent(e.getEventoId(), id -> new Candidato(e))
                                .sumar(PUNTOS_COCOMPRA * cc.getValue().intValue(), "Quienes compraron entradas para "
                                        + "eventos que viste también compraron para este (" + cc.getValue() + ")"));
            }
        }

        // 3) tipos de evento: los de las preferencias + los que ya compro
        Set<String> tipos = new HashSet<>(pref.map(Preferencias::getTiposEvento).orElse(List.of()));
        if (!comprados.isEmpty()) {
            eventos.findAllById(comprados).forEach(e -> tipos.add(e.getTipo()));
        }
        String ciudad = pref.map(Preferencias::getCiudadNormalizada).orElse(null);
        if (!tipos.isEmpty()) {
            Query q = Query.query(Criteria.where("tipo").in(tipos).and("cancelado").is(false).and("fechaHora").gt(ahora))
                    .with(Sort.by("fechaHora")).limit(200);
            for (EventoCatalogo e : mongo.find(q, EventoCatalogo.class)) {
                boolean enSuCiudad = ciudad != null && ciudad.equals(e.getCiudadNormalizada());
                candidatos.computeIfAbsent(e.getEventoId(), id -> new Candidato(e)).sumar(
                        enSuCiudad ? PUNTOS_TIPO_EN_CIUDAD : PUNTOS_TIPO,
                        enSuCiudad ? "Te gustan los eventos de tipo " + e.getTipo() + " y es en tu ciudad"
                                : "Te gustan los eventos de tipo " + e.getTipo());
            }
        }

        return candidatos.values().stream()
                .filter(c -> !comprados.contains(c.evento.getEventoId()))
                .sorted(Comparator.comparingInt((Candidato c) -> -c.puntaje)
                        .thenComparing(c -> c.evento.getFechaHora()))
                .limit(limite)
                .map(c -> RecomendacionResponse.de(c.evento, c.puntaje, c.motivos))
                .toList();
    }

    /** "Quienes compraron entradas para este evento también compraron para..." (publico). */
    public List<TambienCompraronResponse> tambienCompraron(Long eventoId, int limite) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<TambienCompraronResponse> resultado = new ArrayList<>();
        for (Map.Entry<Long, Long> cc : coCompras(Set.of(eventoId), null).entrySet()) {
            eventos.findById(cc.getKey()).filter(e -> e.vigente(ahora)).ifPresent(e -> resultado.add(
                    new TambienCompraronResponse(e.getEventoId(), e.getNombre(), e.getTipo(), e.getCiudad(),
                            e.getFechaHora(), cc.getValue())));
            if (resultado.size() == limite) {
                break;
            }
        }
        return resultado;
    }

    /**
     * Aggregation en MongoDB: busca los clientes que compraron alguno de esos eventos y cuenta, para
     * cada otro evento, cuantos de esos clientes lo compraron. Devuelve eventoId -> compradores en
     * comun, ordenado de mayor a menor.
     */
    Map<Long, Long> coCompras(Set<Long> eventosBase, Long excluirUsuario) {
        Criteria compradoresDeBase = Criteria.where("eventoId").in(eventosBase);
        if (excluirUsuario != null) {
            compradoresDeBase = compradoresDeBase.and("usuarioId").ne(excluirUsuario);
        }
        List<Long> compradores = mongo.findDistinct(Query.query(compradoresDeBase), "usuarioId",
                CompraHistorial.class, Long.class);
        if (compradores.isEmpty()) {
            return Map.of();
        }
        Aggregation agg = newAggregation(
                match(Criteria.where("usuarioId").in(compradores).and("eventoId").nin(eventosBase)),
                group("eventoId").addToSet("usuarioId").as("usuarios"),
                project().and("usuarios").size().as("compradores"),
                sort(Sort.by(Sort.Direction.DESC, "compradores").and(Sort.by("_id"))),
                limit(50));
        Map<Long, Long> resultado = new LinkedHashMap<>();
        for (Document d : mongo.aggregate(agg, CompraHistorial.class, Document.class).getMappedResults()) {
            resultado.put(((Number) d.get("_id")).longValue(), ((Number) d.get("compradores")).longValue());
        }
        return resultado;
    }

    private static final class Candidato {
        final EventoCatalogo evento;
        int puntaje;
        final List<String> motivos = new ArrayList<>();

        Candidato(EventoCatalogo evento) {
            this.evento = evento;
        }

        void sumar(int puntos, String motivo) {
            puntaje += puntos;
            motivos.add(motivo);
        }
    }
}
