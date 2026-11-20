package com.ticketya.gateway.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El gateway es el punto donde se autentica y autoriza: valida la firma y el vencimiento del JWT
 * emitido por ms-usuarios y aplica la matriz de {@link ReglasDeAcceso}. Los microservicios no estan
 * expuestos fuera de la red de Docker, asi que solo reciben requests que pasaron por aca.
 */
@Configuration
@EnableWebFluxSecurity
public class SeguridadConfig {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${jwt.secreto}") String secreto, @Value("${jwt.emisor}") String emisor) {
        SecretKeySpec clave = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(clave)
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(emisor));
        return decoder;
    }

    @Bean
    public SecurityWebFilterChain seguridad(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(ex -> {
                    for (ReglasDeAcceso.Regla r : ReglasDeAcceso.PUBLICAS) {
                        (r.method() == null ? ex.pathMatchers(r.patrones()) : ex.pathMatchers(r.method(), r.patrones()))
                                .permitAll();
                    }
                    for (ReglasDeAcceso.ReglaConRoles r : ReglasDeAcceso.CON_ROLES) {
                        String[] roles = r.roles().toArray(String[]::new);
                        (r.method() == null ? ex.pathMatchers(r.patrones()) : ex.pathMatchers(r.method(), r.patrones()))
                                .hasAnyRole(roles);
                    }
                    ex.anyExchange().authenticated();
                })
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(convertidor()))
                        .authenticationEntryPoint((exchange, e) -> error(exchange, HttpStatus.UNAUTHORIZED,
                                "Falta el token o es inválido/vencido. Iniciá sesión en /api/auth/login"))
                        .accessDeniedHandler((exchange, e) -> error(exchange, HttpStatus.FORBIDDEN,
                                "Tu rol no tiene permiso para esta operación")))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((exchange, ex) -> error(exchange, HttpStatus.UNAUTHORIZED,
                                "Falta el token o es inválido/vencido. Iniciá sesión en /api/auth/login"))
                        .accessDeniedHandler((exchange, ex) -> error(exchange, HttpStatus.FORBIDDEN,
                                "Tu rol no tiene permiso para esta operación")));
        return http.build();
    }

    /** Claim "rol" -> authority ROLE_<rol>. */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> convertidor() {
        return jwt -> {
            String rol = jwt.getClaimAsString("rol");
            List<SimpleGrantedAuthority> authorities = rol == null ? List.of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
            return Mono.just(new JwtAuthenticationToken(jwt, authorities, jwt.getSubject()));
        };
    }

    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String mensaje) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", mensaje);
        body.put("path", exchange.getRequest().getPath().value());
        try {
            byte[] bytes = mapper.writeValueAsBytes(body);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            return response.setComplete();
        }
    }
}
