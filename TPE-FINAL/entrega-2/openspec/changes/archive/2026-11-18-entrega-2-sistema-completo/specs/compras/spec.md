## ADDED Requirements

### Requirement: Resiliencia ante la pasarela
Las llamadas de ms-compras a ms-pagos DEBEN tener timeout, reintentos con la misma clave de idempotencia y un
circuit breaker. Una compra que no se pudo completar DEBE quedar en ERROR y un job de recuperación DEBE
terminarla (completar si se cobró, cerrar como RECHAZADA y liberar la reserva si no).

#### Scenario: Falla transitoria
- **WHEN** la pasarela falla en el primer intento y responde en el segundo
- **THEN** la compra se aprueba y se cobra una sola vez

#### Scenario: Circuito abierto
- **WHEN** la pasarela falla de forma sostenida y el circuito se abre
- **THEN** las compras fallan rápido con 503 sin llamar a la pasarela y quedan en ERROR para el job

#### Scenario: Recuperación
- **WHEN** el job encuentra una compra en ERROR cuyo pago fue aprobado
- **THEN** confirma la reserva y emite las entradas; si no hubo cobro, la cierra como RECHAZADA

### Requirement: Mensaje de compra confirmada
Al aprobarse una compra, ms-compras DEBE publicar `compra.confirmada` mediante un outbox transaccional, para
que notificaciones y recomendaciones se enteren aunque el broker esté caído en ese momento.

#### Scenario: Broker caído
- **WHEN** RabbitMQ no está disponible al aprobarse una compra
- **THEN** la compra se aprueba igual y el mensaje se publica cuando el broker vuelve
