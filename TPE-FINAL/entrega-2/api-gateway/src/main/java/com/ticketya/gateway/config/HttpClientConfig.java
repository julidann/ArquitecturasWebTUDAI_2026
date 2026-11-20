package com.ticketya.gateway.config;

import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El cliente HTTP del gateway (Reactor Netty) por defecto usa su propio resolver de DNS con cache.
 * En docker-compose, si un contenedor se reinicia cambia de IP y el gateway seguia mandando a la IP
 * vieja ("Connection refused"). Con el resolver de la JVM (cache corta, ver JAVA_OPTS) se vuelve a
 * resolver el nombre del servicio, y con varias instancias (ms-reservas) se reparte entre ellas.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public HttpClientCustomizer resolverDeLaJvm() {
        return httpClient -> httpClient.resolver(DefaultAddressResolverGroup.INSTANCE);
    }
}
