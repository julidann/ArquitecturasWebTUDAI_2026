package com.ticketya.eventos.service;

import com.ticketya.eventos.dto.EventoResponse;
import com.ticketya.eventos.dto.FiltroBusqueda;
import com.ticketya.eventos.dto.OrdenBusqueda;
import com.ticketya.eventos.dto.PaginaResponse;
import com.ticketya.eventos.model.EstadoEvento;
import com.ticketya.eventos.model.Evento;
import com.ticketya.eventos.repository.EventoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.ticketya.eventos.repository.EventoSpecifications.*;

/**
 * Busqueda de eventos por multiples criterios (funcionalidad 9).
 *
 * Solo se muestran eventos PUBLICADOS o AGOTADOS (los borradores, suspendidos, etc. no son
 * visibles para el publico). El ordenamiento por fecha y precio se resuelve en la base;
 * el de relevancia se calcula en memoria con un puntaje simple de coincidencias de texto.
 */
@Service
public class BusquedaService {

    static final List<EstadoEvento> ESTADOS_VISIBLES = List.of(EstadoEvento.PUBLICADO, EstadoEvento.AGOTADO);
    static final int TAMANIO_MAXIMO = 50;

    private final EventoRepository eventoRepository;

    public BusquedaService(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<EventoResponse> buscar(FiltroBusqueda f) {
        int tamanio = Math.min(Math.max(f.tamanio(), 1), TAMANIO_MAXIMO);
        int pagina = Math.max(f.pagina(), 0);
        if (f.desde() != null && f.hasta() != null && f.desde().isAfter(f.hasta())) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }

        Specification<Evento> spec = Specification.where(conEstadoEn(ESTADOS_VISIBLES))
                .and(deTipo(f.tipo()))
                .and(deOrganizador(f.organizadorId()))
                .and(conParticipante(f.participante()))
                .and(enCiudad(f.ciudad()))
                .and(desde(f.desde() == null ? null : f.desde().atStartOfDay()))
                .and(hasta(f.hasta() == null ? null : f.hasta().atTime(23, 59, 59)));

        if (f.orden() == OrdenBusqueda.RELEVANCIA) {
            return buscarPorRelevancia(spec, f, pagina, tamanio);
        }

        // el texto libre filtra por nombre/descripcion cuando no se ordena por relevancia
        spec = spec.and(conTexto(f.texto()));
        Sort.Direction dir = f.descendente() ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = switch (f.orden()) {
            case PRECIO -> Sort.by(dir, "precioMinimo").and(Sort.by("fechaHora"));
            default -> Sort.by(dir, "fechaHora");
        };
        Page<Evento> page = eventoRepository.findAll(spec, PageRequest.of(pagina, tamanio, sort));
        return new PaginaResponse<>(page.getContent().stream().map(EventoResponse::de).toList(),
                pagina, tamanio, page.getTotalElements(), page.getTotalPages());
    }

    private PaginaResponse<EventoResponse> buscarPorRelevancia(Specification<Evento> spec, FiltroBusqueda f,
                                                               int pagina, int tamanio) {
        String[] terminos = terminos(f.texto());
        List<Evento> ordenados = eventoRepository.findAll(spec).stream()
                .map(e -> new Puntuado(e, puntaje(e, terminos)))
                .filter(p -> terminos.length == 0 || p.puntaje() > 0)
                .sorted(Comparator.comparingInt(Puntuado::puntaje).reversed()
                        .thenComparing(p -> p.evento().getFechaHora()))
                .map(Puntuado::evento)
                .toList();
        int desde = Math.min(pagina * tamanio, ordenados.size());
        int hasta = Math.min(desde + tamanio, ordenados.size());
        int totalPaginas = (int) Math.ceil((double) ordenados.size() / tamanio);
        return new PaginaResponse<>(ordenados.subList(desde, hasta).stream().map(EventoResponse::de).toList(),
                pagina, tamanio, ordenados.size(), totalPaginas);
    }

    /**
     * Puntaje de relevancia: cada termino suma 3 si aparece en el nombre, 2 si aparece en
     * algun participante y 1 si aparece en la descripcion. Ademas se suma 1 si el evento es
     * en los proximos 30 dias, para priorizar lo mas cercano.
     */
    int puntaje(Evento e, String[] terminos) {
        int puntos = 0;
        String nombre = normalizar(e.getNombre());
        String descripcion = normalizar(e.getDescripcion());
        String participantes = normalizar(String.join(" ", e.getParticipantes()));
        for (String t : terminos) {
            if (nombre.contains(t)) puntos += 3;
            if (participantes.contains(t)) puntos += 2;
            if (descripcion.contains(t)) puntos += 1;
        }
        if (terminos.length > 0 && puntos == 0) {
            return 0;
        }
        if (e.getFechaHora().isBefore(LocalDateTime.now().plusDays(30))) {
            puntos += 1;
        }
        return puntos;
    }

    private static String[] terminos(String texto) {
        if (texto == null || texto.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(normalizar(texto).split("\\s+")).filter(t -> t.length() > 1).toArray(String[]::new);
    }

    private static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        return java.text.Normalizer.normalize(s.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private record Puntuado(Evento evento, int puntaje) {
    }
}
