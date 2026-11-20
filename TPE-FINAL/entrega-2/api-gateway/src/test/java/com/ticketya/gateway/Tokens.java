package com.ticketya.gateway;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Arma JWT como los que emite ms-usuarios, para probar el gateway. */
public final class Tokens {

    public static final String SECRETO = "clave-de-test-del-gateway-con-mas-de-32-bytes";

    private Tokens() {
    }

    public static String de(long usuarioId, String rol) {
        return firmar(usuarioId, rol, SECRETO, Instant.now().plus(Duration.ofHours(1)), "ticketya");
    }

    public static String vencido(long usuarioId, String rol) {
        return firmar(usuarioId, rol, SECRETO, Instant.now().minus(Duration.ofHours(1)), "ticketya");
    }

    public static String conOtraClave(long usuarioId, String rol) {
        return firmar(usuarioId, rol, "otra-clave-que-no-es-la-del-gateway-123456", Instant.now().plus(Duration.ofHours(1)),
                "ticketya");
    }

    public static String firmar(long usuarioId, String rol, String secreto, Instant expira, String emisor) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(emisor)
                    .subject(String.valueOf(usuarioId))
                    .claim("rol", rol)
                    .claim("email", "u" + usuarioId + "@test.com")
                    .issueTime(Date.from(expira.minus(Duration.ofHours(2))))
                    .expirationTime(Date.from(expira))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secreto.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
