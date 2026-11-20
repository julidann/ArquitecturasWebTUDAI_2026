# Entrega 2: sistema completo

## Why
La entrega 1 dejó el core (eventos, búsqueda, reserva y compra sin doble venta, devoluciones). Para la entrega 2
faltan las funcionalidades 6, 8, 10, 11 y 12 de la consigna y los requisitos no funcionales: seguridad con
JWT y roles, consistencia entre servicios, resiliencia, cache, log centralizado, mensajería asincrónica,
despliegue con un contenedor por servicio y pruebas de integración, de roles y de carga.

## What Changes
- **Nuevos microservicios:** `ms-usuarios` (registro, login con JWT), `ms-fila` (fila virtual en Redis),
  `ms-recomendaciones` (preferencias y recomendaciones en MongoDB), `ms-notificaciones` (avisos por email
  simulado, MongoDB) y `ms-bitacora` (log centralizado en MongoDB).
- **ms-compras:** control de acceso con QR, reportes para el organizador, saga de compra con circuit breaker,
  reintentos y job de recuperación, outbox para `compra.confirmada`.
- **ms-eventos:** reprogramación de eventos, cache en Redis del detalle y de las búsquedas, outbox para
  `evento.publicado`, `evento.cancelado` y `evento.reprogramado`.
- **ms-reservas:** exige turno de la fila en eventos de alta demanda.
- **api-gateway:** valida el JWT, autoriza por rol, pisa los headers de identidad y genera el correlation id.
- **Mensajería:** RabbitMQ con exchanges `ticketya.eventos` (mensajes de negocio), `ticketya.bitacora` y `ticketya.dlx`,
  consumidores idempotentes y colas de error (DLQ).
- **Despliegue:** un `Dockerfile` por servicio y un único `docker-compose.yml` con todo el sistema
  (ms-reservas con 2 instancias).
- **BREAKING:** los endpoints dejan de aceptar los headers `X-Usuario-Id` / `X-Rol` del cliente; hace falta un
  JWT (`Authorization: Bearer`).

## Impact
- Specs nuevas: `fila-virtual`, `control-acceso`, `recomendaciones`, `notificaciones`, `reportes`, `seguridad`,
  `trazabilidad`.
- Specs modificadas (requisitos agregados): `reservas`, `compras`, `gestion-eventos`, `busqueda-eventos`,
  `devoluciones`.
- Documentación: `docs/01` a `docs/07`, `docs/openapi/`, colección Postman, README.
