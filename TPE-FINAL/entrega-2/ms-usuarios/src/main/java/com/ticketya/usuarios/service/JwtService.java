package com.ticketya.usuarios.service;

import com.ticketya.usuarios.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Emite los JWT (HS256). El api-gateway valida la firma con la misma clave y saca del token el
 * id de usuario (claim "sub") y el rol (claim "rol"), que reenvia a los microservicios en los
 * headers X-Usuario-Id y X-Rol.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final Duration vigencia;
    private final String emisor;
    private final Clock clock;

    public JwtService(@Value("${jwt.secreto}") String secreto, @Value("${jwt.horas-vigencia:8}") long horas,
                      @Value("${jwt.emisor:ticketya}") String emisor, Clock clock) {
        if (secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secreto tiene que tener al menos 32 bytes para HS256");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.vigencia = Duration.ofHours(horas);
        this.emisor = emisor;
        this.clock = clock;
    }

    public TokenEmitido emitir(Usuario usuario) {
        Instant ahora = clock.instant();
        Instant expira = ahora.plus(vigencia);
        String token = Jwts.builder()
                .issuer(emisor)
                .subject(String.valueOf(usuario.getId()))
                .claim("rol", usuario.getRol().name())
                .claim("email", usuario.getEmail())
                .claim("nombre", usuario.getNombre())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
        return new TokenEmitido(token, expira);
    }

    /** Solo se usa en los tests (la validacion real la hace el gateway). */
    public Claims leer(String token) {
        return Jwts.parser().verifyWith(clave).requireIssuer(emisor).build().parseSignedClaims(token).getPayload();
    }

    public record TokenEmitido(String token, Instant expiraEn) {
    }
}
