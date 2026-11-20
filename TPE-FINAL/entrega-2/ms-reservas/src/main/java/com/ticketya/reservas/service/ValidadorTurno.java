package com.ticketya.reservas.service;

import com.ticketya.reservas.exception.AccesoDenegadoException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Valida el turno de la fila virtual (header X-Turno-Fila). El turno lo firma ms-fila con una clave
 * compartida, asi que se valida localmente: la reserva no depende de que ms-fila este disponible.
 */
@Component
public class ValidadorTurno {

    private final SecretKey clave;

    public ValidadorTurno(@Value("${fila.secreto}") String secreto) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    public void validar(String turno, long eventoId, long usuarioId) {
        if (turno == null || turno.isBlank()) {
            throw new AccesoDenegadoException("El evento " + eventoId + " es de alta demanda: primero ingresá a la "
                    + "fila virtual (POST /api/fila/eventos/" + eventoId + "/ingresar) y reservá con tu turno "
                    + "en el header X-Turno-Fila");
        }
        Claims c;
        try {
            c = Jwts.parser().verifyWith(clave).requireIssuer("ms-fila").build().parseSignedClaims(turno).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new AccesoDenegadoException("El turno de la fila virtual es inválido o venció. Volvé a ingresar a la fila.");
        }
        Number ev = c.get("eventoId", Number.class);
        if (ev == null || ev.longValue() != eventoId || !String.valueOf(usuarioId).equals(c.getSubject())) {
            throw new AccesoDenegadoException("El turno de la fila virtual no corresponde a este evento o usuario");
        }
    }
}
