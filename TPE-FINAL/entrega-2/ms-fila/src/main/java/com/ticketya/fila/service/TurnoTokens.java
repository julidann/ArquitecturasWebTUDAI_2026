package com.ticketya.fila.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Firma el "turno" que recibe un cliente cuando lo habilitan. Es un JWT corto (HS256) con el evento,
 * el usuario y el vencimiento. ms-reservas lo valida con la misma clave SIN consultar a este servicio,
 * asi la fila no agrega una llamada remota al camino critico de la reserva.
 */
@Component
public class TurnoTokens {

    private final SecretKey clave;

    public TurnoTokens(@Value("${fila.secreto}") String secreto) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    public String emitir(long eventoId, long usuarioId, Instant vence) {
        return Jwts.builder()
                .issuer("ms-fila")
                .subject(String.valueOf(usuarioId))
                .claim("eventoId", eventoId)
                .issuedAt(new Date())
                .expiration(Date.from(vence))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }
}
