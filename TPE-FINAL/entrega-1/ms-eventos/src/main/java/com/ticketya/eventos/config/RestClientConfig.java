package com.ticketya.eventos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${servicios.timeout-ms:3000}")
    private int timeoutMs;

    @Bean
    public RestClient reservasHttp(@Value("${servicios.reservas.url}") String url) {
        return crear(url);
    }

    @Bean
    public RestClient comprasHttp(@Value("${servicios.compras.url}") String url) {
        return crear(url);
    }

    private RestClient crear(String baseUrl) {
        // cliente HTTP del JDK (el de HttpURLConnection no soporta PATCH)
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
