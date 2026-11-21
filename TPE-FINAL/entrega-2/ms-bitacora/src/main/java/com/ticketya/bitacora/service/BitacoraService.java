package com.ticketya.bitacora.service;

import com.ticketya.bitacora.dto.RegistroResponse;
import com.ticketya.bitacora.mensajeria.RegistroMensaje;
import com.ticketya.bitacora.model.Registro;
import com.ticketya.bitacora.repository.RegistroRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BitacoraService {

    private final RegistroRepository repository;
    private final MongoTemplate mongo;
    private final Clock clock;

    public BitacoraService(RegistroRepository repository, MongoTemplate mongo, Clock clock) {
        this.repository = repository;
        this.mongo = mongo;
        this.clock = clock;
    }

    public void guardar(RegistroMensaje m) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        repository.save(new Registro(m.correlationId(), m.servicio(), m.operacion(), m.resultado(), m.usuarioId(),
                m.detalle() == null ? Map.of() : m.detalle(), m.fecha() == null ? ahora : m.fecha(), ahora));
    }

    /** Busqueda con filtros opcionales, de lo mas nuevo a lo mas viejo. */
    public List<RegistroResponse> buscar(Filtro f) {
        List<Criteria> condiciones = new ArrayList<>();
        if (f.correlationId() != null) condiciones.add(Criteria.where("correlationId").is(f.correlationId()));
        if (f.operacion() != null) {
            // "compra" encuentra compra.aprobada, compra.rechazada, ...
            condiciones.add(f.operacion().contains(".") ? Criteria.where("operacion").is(f.operacion())
                    : Criteria.where("operacion").regex("^" + java.util.regex.Pattern.quote(f.operacion()) + "\\."));
        }
        if (f.servicio() != null) condiciones.add(Criteria.where("servicio").is(f.servicio()));
        if (f.usuarioId() != null) condiciones.add(Criteria.where("usuarioId").is(f.usuarioId()));
        if (f.desde() != null) condiciones.add(Criteria.where("fecha").gte(f.desde()));
        if (f.hasta() != null) condiciones.add(Criteria.where("fecha").lte(f.hasta()));
        Query q = condiciones.isEmpty() ? new Query() : Query.query(new Criteria().andOperator(condiciones));
        q.with(Sort.by(Sort.Direction.DESC, "fecha")).limit(f.limite());
        return mongo.find(q, Registro.class).stream().map(RegistroResponse::de).toList();
    }

    /** Todos los pasos de una operacion, en orden, a traves de los distintos servicios. */
    public List<RegistroResponse> traza(String correlationId) {
        return repository.findByCorrelationIdOrderByFechaAsc(correlationId).stream().map(RegistroResponse::de).toList();
    }

    public record Filtro(String correlationId, String operacion, String servicio, Long usuarioId,
                         LocalDateTime desde, LocalDateTime hasta, int limite) {
    }
}
