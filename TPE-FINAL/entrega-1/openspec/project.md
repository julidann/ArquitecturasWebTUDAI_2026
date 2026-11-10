# Contexto del proyecto

## Propósito
TicketYa es una plataforma de venta de entradas para eventos (recitales, festivales, teatro, deportes,
conferencias). Organizadores publican eventos, clientes buscan y compran entradas desde el celular, y
en el ingreso el personal de acceso valida el QR. El desafío principal es la apertura de venta de eventos
masivos: el sistema tiene que seguir funcionando, ser justo y **nunca vender dos veces la misma entrada**.

Trabajo Práctico Especial de Arquitectura Web (TUDAI, 2026). Este repositorio contiene el backend.

## Stack tecnológico
- Java 21, Spring Boot 3.3, Spring Data JPA, Bean Validation
- Spring Cloud Gateway como API Gateway
- PostgreSQL 16 (una base por microservicio), H2 para tests y para correr sin Docker
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, AssertJ, Testcontainers
- Docker / docker-compose (en la entrega 1 solo para las bases)

## Convenciones
- Código, endpoints, entidades y mensajes en **español** (es el idioma del dominio y del equipo).
- Un proyecto Maven independiente por microservicio, paquete base `com.ticketya.<servicio>`.
- Capas: `controller` → `service` → `repository`; DTOs como `record` en `dto`; errores con un
  `GlobalExceptionHandler` por servicio y un formato común `ErrorResponse`.
- Comunicación entre servicios con `RestClient` síncrono y timeouts cortos.
- Endpoints que solo usan otros servicios van bajo `/internal/**` o están documentados como `[Interno]`
  y el gateway no los expone.
- Commits en español, en presente o infinitivo ("agrega", "corrige", ...).

## Restricciones del dominio
- Una entrada nunca puede venderse dos veces, aunque haya varias instancias del mismo servicio.
- La reserva temporal de lugares dura 10 minutos.
- Límite de entradas por cliente para un mismo evento (configurable por evento, 6 por defecto).
- La devolución se puede pedir hasta 72 horas antes del evento.
- Entrega 1 sin autenticación: el usuario viaja en los headers `X-Usuario-Id` y `X-Rol`.

## Proceso (spec-driven)
Cada funcionalidad arranca como un *change* en `openspec/changes/<id>/` con:
`proposal.md` (por qué y qué) → `specs/` (requisitos y escenarios) → `design.md` (cómo) → `tasks.md`.
Se revisa en grupo, se implementa y al terminar se archiva en `changes/archive/` y los requisitos pasan a
`openspec/specs/`, que es la fuente de verdad del comportamiento actual. Los escenarios de las specs son
la base de los contratos OpenAPI (`docs/openapi/`) y de los casos de prueba (ver `docs/05-concurrencia-y-pruebas.md`).
