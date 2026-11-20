# Tareas

## 1. Seguridad
- [x] 1.1 ms-usuarios: registro (siempre CLIENTE), login con JWT, alta de usuarios con rol por un ADMIN, usuarios de prueba
- [x] 1.2 api-gateway: validación del JWT, matriz de permisos por rol, pisar headers de identidad, endpoints internos ocultos
- [x] 1.3 `SeguridadEndpointsTest`: cada endpoint con cada rol y sin token

## 2. Funcionalidades
- [x] 2.1 ms-fila: apertura por mensaje, ingreso, tandas con lock, turno firmado, configuración, abandono, cierre
- [x] 2.2 ms-reservas: validar `X-Turno-Fila` en eventos de alta demanda
- [x] 2.3 ms-compras: validación de QR (concurrente), registro y resumen de accesos
- [x] 2.4 ms-recomendaciones: preferencias, catálogo y historial por mensajes, recomendaciones, "también compraron"
- [x] 2.5 ms-notificaciones: avisos de compra, cancelación, reprogramación y artista favorito; reintentos
- [x] 2.6 ms-compras: reporte por sector para el organizador
- [x] 2.7 ms-eventos: reprogramación

## 3. Consistencia, resiliencia, cache y trazabilidad
- [x] 3.1 Outbox en ms-eventos y ms-compras; RabbitMQ con DLQ; consumidores idempotentes
- [x] 3.2 Saga de compra con compensación y `RecuperacionComprasJob`
- [x] 3.3 Resilience4j (timeouts, retry, circuit breaker) en ms-compras y ms-notificaciones
- [x] 3.4 Cache Redis en ms-eventos
- [x] 3.5 Correlation id + ms-bitacora (MongoDB) + log de requests en todos los servicios

## 4. Despliegue
- [x] 4.1 Dockerfile multi-etapa por servicio
- [x] 4.2 docker-compose único (servicios + PostgreSQL, MongoDB, Redis, RabbitMQ), ms-reservas ×2

## 5. Pruebas
- [x] 5.1 Tests unitarios / de componentes de los servicios nuevos (Testcontainers para Redis y MongoDB)
- [x] 5.2 `tests-integracion/` contra el sistema completo
- [x] 5.3 Prueba de carga con JMeter e informe (`docs/07`)

## 6. Documentación
- [x] 6.1 Criterios de aceptación US-19 a US-29
- [x] 6.2 Diagrama de módulos, ER, estrategias de consistencia, decisiones tecnológicas
- [x] 6.3 OpenAPI de los 9 servicios, colección Postman, README
