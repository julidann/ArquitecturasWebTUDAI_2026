package com.ticketya.eventos.repository;

import com.ticketya.eventos.model.EstadoEvento;
import com.ticketya.eventos.model.Evento;
import com.ticketya.eventos.model.TipoEvento;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Filtros combinables para la busqueda de eventos (funcionalidad 9).
 * Cada metodo devuelve null si el parametro no vino, asi Specification.where los ignora.
 */
public final class EventoSpecifications {

    private EventoSpecifications() {
    }

    public static Specification<Evento> conEstadoEn(Collection<EstadoEvento> estados) {
        return (root, query, cb) -> estados == null || estados.isEmpty() ? null : root.get("estado").in(estados);
    }

    public static Specification<Evento> deTipo(TipoEvento tipo) {
        return (root, query, cb) -> tipo == null ? null : cb.equal(root.get("tipo"), tipo);
    }

    public static Specification<Evento> deOrganizador(Long organizadorId) {
        return (root, query, cb) -> organizadorId == null ? null : cb.equal(root.get("organizadorId"), organizadorId);
    }

    public static Specification<Evento> conParticipante(String participante) {
        return (root, query, cb) -> {
            if (participante == null || participante.isBlank()) {
                return null;
            }
            query.distinct(true);
            Join<Evento, String> p = root.join("participantes");
            return cb.like(cb.lower(p), "%" + participante.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Evento> enCiudad(String ciudad) {
        return (root, query, cb) -> ciudad == null || ciudad.isBlank() ? null
                : cb.equal(cb.lower(root.get("lugar").get("ciudad")), ciudad.toLowerCase().trim());
    }

    public static Specification<Evento> desde(LocalDateTime desde) {
        return (root, query, cb) -> desde == null ? null : cb.greaterThanOrEqualTo(root.get("fechaHora"), desde);
    }

    public static Specification<Evento> hasta(LocalDateTime hasta) {
        return (root, query, cb) -> hasta == null ? null : cb.lessThanOrEqualTo(root.get("fechaHora"), hasta);
    }

    /** Busqueda libre por texto en nombre o descripcion. */
    public static Specification<Evento> conTexto(String texto) {
        return (root, query, cb) -> {
            if (texto == null || texto.isBlank()) {
                return null;
            }
            String patron = "%" + texto.toLowerCase().trim() + "%";
            return cb.or(cb.like(cb.lower(root.get("nombre")), patron),
                    cb.like(cb.lower(root.get("descripcion")), patron));
        };
    }
}
