package com.ticketya.fila.service;

import com.ticketya.fila.dto.*;
import com.ticketya.fila.exception.AccesoDenegadoException;
import com.ticketya.fila.exception.OperacionInvalidaException;
import com.ticketya.fila.exception.RecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Fila virtual (funcionalidad 6) sobre Redis. Todo el estado vive en Redis, asi que pueden correr
 * varias instancias del servicio a la vez:
 *
 * <pre>
 * fila:{ev}:config            hash  activa, tamanioTanda, intervaloSegundos, minutosParaComprar, organizadorId
 * fila:{ev}:seq               contador de llegada (INCR atomico -> orden justo)
 * fila:{ev}:espera            sorted set usuario -> numero de llegada
 * fila:{ev}:hab:{usuario}     turno firmado, con TTL = minutos para comprar
 * fila:{ev}:habilitados       contador de habilitados (estadistica)
 * fila:{ev}:tanda             "lock" con TTL = intervalo: solo una instancia habilita cada tanda
 * filas:activas               set con los eventos que tienen fila abierta
 * </pre>
 *
 * Simplificaciones (justificadas en docs/06): no hay "sala de espera" previa a la apertura ni
 * deteccion de bots; el orden es el de llegada al endpoint de ingreso.
 */
@Service
public class FilaService {

    private static final Logger log = LoggerFactory.getLogger(FilaService.class);
    static final String ACTIVAS = "filas:activas";

    private final StringRedisTemplate redis;
    private final TurnoTokens tokens;
    private final Clock clock;
    private final int tamanioDefault;
    private final int intervaloDefault;
    private final int minutosDefault;

    public FilaService(StringRedisTemplate redis, TurnoTokens tokens, Clock clock,
                       @Value("${fila.tamanio-tanda:100}") int tamanioDefault,
                       @Value("${fila.intervalo-segundos:10}") int intervaloDefault,
                       @Value("${fila.minutos-para-comprar:10}") int minutosDefault) {
        this.redis = redis;
        this.tokens = tokens;
        this.clock = clock;
        this.tamanioDefault = tamanioDefault;
        this.intervaloDefault = intervaloDefault;
        this.minutosDefault = minutosDefault;
    }

    /** Se abre al publicarse un evento de alta demanda (mensaje evento.publicado). Idempotente. */
    public void abrir(long eventoId, Long organizadorId) {
        String k = config(eventoId);
        redis.opsForHash().putIfAbsent(k, "tamanioTanda", String.valueOf(tamanioDefault));
        redis.opsForHash().putIfAbsent(k, "intervaloSegundos", String.valueOf(intervaloDefault));
        redis.opsForHash().putIfAbsent(k, "minutosParaComprar", String.valueOf(minutosDefault));
        if (organizadorId != null) {
            redis.opsForHash().putIfAbsent(k, "organizadorId", String.valueOf(organizadorId));
        }
        redis.opsForHash().put(k, "activa", "true");
        redis.opsForSet().add(ACTIVAS, String.valueOf(eventoId));
        log.info("Fila virtual abierta para el evento {}", eventoId);
    }

    /** Evento cancelado: nadie mas puede entrar ni ser habilitado. */
    public void cerrar(long eventoId) {
        if (Boolean.TRUE.equals(redis.hasKey(config(eventoId)))) {
            redis.opsForHash().put(config(eventoId), "activa", "false");
        }
        redis.opsForSet().remove(ACTIVAS, String.valueOf(eventoId));
        log.info("Fila virtual del evento {} cerrada", eventoId);
    }

    public TurnoResponse ingresar(long eventoId, long usuarioId) {
        Config c = leerConfig(eventoId);
        if (!c.activa()) {
            throw new OperacionInvalidaException("La fila virtual del evento " + eventoId + " está cerrada");
        }
        TurnoResponse habilitado = siHabilitado(eventoId, usuarioId);
        if (habilitado != null) {
            return habilitado;
        }
        String u = String.valueOf(usuarioId);
        if (redis.opsForZSet().score(espera(eventoId), u) == null) {
            Long llegada = redis.opsForValue().increment(seq(eventoId));
            // addIfAbsent = ZADD NX: si el mismo cliente entra dos veces a la vez conserva el primer lugar
            redis.opsForZSet().addIfAbsent(espera(eventoId), u, llegada);
        }
        return enEspera(eventoId, usuarioId, c);
    }

    public TurnoResponse miTurno(long eventoId, long usuarioId) {
        Config c = leerConfig(eventoId);
        TurnoResponse habilitado = siHabilitado(eventoId, usuarioId);
        if (habilitado != null) {
            return habilitado;
        }
        if (redis.opsForZSet().score(espera(eventoId), String.valueOf(usuarioId)) == null) {
            throw new RecursoNoEncontradoException("No estás en la fila del evento " + eventoId
                    + " (o tu turno ya venció). Volvé a ingresar.");
        }
        return enEspera(eventoId, usuarioId, c);
    }

    public void abandonar(long eventoId, long usuarioId) {
        redis.opsForZSet().remove(espera(eventoId), String.valueOf(usuarioId));
        redis.delete(habilitado(eventoId, usuarioId));
    }

    /**
     * Habilita la siguiente tanda si ya paso el intervalo. El SET NX con TTL hace de lock distribuido:
     * aunque haya N instancias corriendo el job, solo una habilita cada tanda. ZPOPMIN es atomico, asi
     * que ningun cliente se habilita dos veces ni se saltea.
     *
     * @return cantidad de clientes habilitados (0 si no tocaba)
     */
    public int habilitarTanda(long eventoId) {
        Config c = leerConfig(eventoId);
        if (!c.activa()) {
            return 0;
        }
        Boolean meToca = redis.opsForValue().setIfAbsent(tanda(eventoId), "1", Duration.ofSeconds(c.intervaloSegundos()));
        if (!Boolean.TRUE.equals(meToca)) {
            return 0;
        }
        Set<ZSetOperations.TypedTuple<String>> siguientes = redis.opsForZSet().popMin(espera(eventoId), c.tamanioTanda());
        if (siguientes == null || siguientes.isEmpty()) {
            return 0;
        }
        Instant vence = clock.instant().plus(Duration.ofMinutes(c.minutosParaComprar()));
        for (ZSetOperations.TypedTuple<String> t : siguientes) {
            long usuario = Long.parseLong(Objects.requireNonNull(t.getValue()));
            redis.opsForValue().set(habilitado(eventoId, usuario), tokens.emitir(eventoId, usuario, vence),
                    Duration.ofMinutes(c.minutosParaComprar()));
        }
        redis.opsForValue().increment(clave(eventoId, "habilitados"), siguientes.size());
        log.info("Fila del evento {}: tanda de {} clientes habilitada", eventoId, siguientes.size());
        return siguientes.size();
    }

    public List<Long> filasActivas() {
        Set<String> ids = redis.opsForSet().members(ACTIVAS);
        return ids == null ? List.of() : ids.stream().map(Long::valueOf).sorted().toList();
    }

    public EstadoFilaResponse estado(long eventoId, Long usuarioId, String rol) {
        Config c = leerConfig(eventoId);
        exigirDuenio(c, usuarioId, rol);
        Long enEspera = redis.opsForZSet().zCard(espera(eventoId));
        String hab = redis.opsForValue().get(clave(eventoId, "habilitados"));
        return new EstadoFilaResponse(eventoId, c.activa(), enEspera == null ? 0 : enEspera,
                hab == null ? 0 : Long.parseLong(hab), c.tamanioTanda(), c.intervaloSegundos(), c.minutosParaComprar());
    }

    public EstadoFilaResponse configurar(long eventoId, ConfiguracionRequest req, Long usuarioId, String rol) {
        boolean existe = Boolean.TRUE.equals(redis.hasKey(config(eventoId)));
        if (existe) {
            exigirDuenio(leerConfig(eventoId), usuarioId, rol);
        } else if (!"ADMIN".equals(rol)) {
            throw new RecursoNoEncontradoException("El evento " + eventoId + " no tiene fila virtual "
                    + "(se crea al publicar un evento de alta demanda)");
        }
        Map<String, String> valores = new HashMap<>();
        valores.put("tamanioTanda", String.valueOf(req.tamanioTanda()));
        valores.put("intervaloSegundos", String.valueOf(req.intervaloSegundos()));
        valores.put("minutosParaComprar", String.valueOf(req.minutosParaComprar()));
        redis.opsForHash().putAll(config(eventoId), valores);
        if (req.activa() == null || req.activa()) {
            redis.opsForHash().put(config(eventoId), "activa", "true");
            redis.opsForSet().add(ACTIVAS, String.valueOf(eventoId));
        } else {
            cerrar(eventoId);
        }
        return estado(eventoId, usuarioId, "ADMIN");
    }

    private TurnoResponse siHabilitado(long eventoId, long usuarioId) {
        String k = habilitado(eventoId, usuarioId);
        String turno = redis.opsForValue().get(k);
        if (turno == null) {
            return null;
        }
        Long ttl = redis.getExpire(k);
        Instant hasta = clock.instant().plusSeconds(ttl == null || ttl < 0 ? 0 : ttl);
        return new TurnoResponse(eventoId, usuarioId, EstadoTurno.HABILITADO, null,
                redis.opsForZSet().zCard(espera(eventoId)), 0L, turno, hasta);
    }

    private TurnoResponse enEspera(long eventoId, long usuarioId, Config c) {
        Long rank = redis.opsForZSet().rank(espera(eventoId), String.valueOf(usuarioId));
        long posicion = rank == null ? 1 : rank + 1;
        long tandasAntes = (posicion - 1) / c.tamanioTanda();
        long estimado = (tandasAntes + 1) * c.intervaloSegundos();
        return new TurnoResponse(eventoId, usuarioId, EstadoTurno.EN_ESPERA, posicion,
                redis.opsForZSet().zCard(espera(eventoId)), estimado, null, null);
    }

    private Config leerConfig(long eventoId) {
        Map<Object, Object> h = redis.opsForHash().entries(config(eventoId));
        if (h.isEmpty()) {
            throw new RecursoNoEncontradoException("El evento " + eventoId + " no tiene fila virtual");
        }
        return new Config("true".equals(h.get("activa")),
                entero(h, "tamanioTanda", tamanioDefault), entero(h, "intervaloSegundos", intervaloDefault),
                entero(h, "minutosParaComprar", minutosDefault),
                h.get("organizadorId") == null ? null : Long.valueOf((String) h.get("organizadorId")));
    }

    private void exigirDuenio(Config c, Long usuarioId, String rol) {
        if ("ADMIN".equals(rol)) {
            return;
        }
        if (!"ORGANIZADOR".equals(rol) || c.organizadorId() == null || !c.organizadorId().equals(usuarioId)) {
            throw new AccesoDenegadoException("Solo el organizador del evento o un administrador");
        }
    }

    private static int entero(Map<Object, Object> h, String campo, int defecto) {
        Object v = h.get(campo);
        return v == null ? defecto : Integer.parseInt((String) v);
    }

    private static String clave(long eventoId, String sufijo) {
        return "fila:" + eventoId + ":" + sufijo;
    }

    private static String config(long e) { return clave(e, "config"); }
    private static String seq(long e) { return clave(e, "seq"); }
    private static String espera(long e) { return clave(e, "espera"); }
    private static String tanda(long e) { return clave(e, "tanda"); }
    private static String habilitado(long e, long u) { return clave(e, "hab:" + u); }

    record Config(boolean activa, int tamanioTanda, int intervaloSegundos, int minutosParaComprar, Long organizadorId) {
    }
}
