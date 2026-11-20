package com.ticketya.usuarios.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OpenAPI usuariosOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketYa - ms-usuarios")
                        .version("2.0.0")
                        .description("""
                                Registro e inicio de sesión. `POST /auth/login` devuelve un **JWT** (HS256) con el id
                                de usuario (`sub`) y el rol (`rol`: CLIENTE, ORGANIZADOR, ADMIN o ACCESO).
                                Ese token se envía en `Authorization: Bearer <token>` en todos los requests al gateway.
                                """))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("A través del API Gateway"),
                        new Server().url("http://localhost:8085").description("Directo al microservicio")));
    }
}
