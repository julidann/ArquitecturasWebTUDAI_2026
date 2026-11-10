# devoluciones Specification

## Purpose
Devolución de entradas a pedido del cliente y reintegros por cancelación de eventos (funcionalidad 7).
Microservicio: `ms-compras` (con `ms-reservas` y `ms-pagos`).

## Requirements

### Requirement: Solicitud de devolución
Un cliente DEBE poder solicitar la devolución de una entrada VALIDA propia hasta 72 horas antes del evento.
No puede haber dos solicitudes pendientes para la misma entrada.

#### Scenario: Dentro del plazo
- **WHEN** el cliente pide la devolución con más de 72 horas de anticipación
- **THEN** se crea la solicitud en estado PENDIENTE con el monto a reintegrar

#### Scenario: Fuera de plazo
- **WHEN** faltan menos de 72 horas para el evento
- **THEN** el sistema responde 422

#### Scenario: Entrada ajena
- **WHEN** el cliente pide la devolución de una entrada de otro
- **THEN** el sistema responde 403

### Requirement: Resolución por un administrador
Un administrador DEBE aprobar o rechazar cada solicitud. Al aprobar, el asiento o lugar en la zona DEBE
liberarse y DEBE solicitarse el reintegro del precio de la entrada a la pasarela de pagos. La entrada pasa a
DEVUELTA. Si alguna de las dos operaciones falla, la solicitud DEBE seguir PENDIENTE y poder reaprobarse sin
liberar ni reintegrar dos veces.

#### Scenario: Aprobar
- **WHEN** un administrador aprueba una solicitud pendiente
- **THEN** el asiento vuelve a DISPONIBLE, se reintegra el precio y la entrada queda DEVUELTA

#### Scenario: No es administrador
- **WHEN** un cliente intenta aprobar una solicitud
- **THEN** el sistema responde 403

#### Scenario: Falla el reintegro
- **WHEN** ms-pagos no responde al aprobar
- **THEN** la solicitud sigue PENDIENTE, la entrada sigue VALIDA y el administrador puede reintentar

#### Scenario: Solicitud ya resuelta
- **WHEN** se intenta aprobar o rechazar una solicitud ya resuelta
- **THEN** el sistema responde 409

### Requirement: Reintegro por cancelación de evento
Cuando un evento se cancela, todas sus entradas válidas DEBEN pasar a CANCELADA con reintegro automático y
DEBE notificarse a los compradores (la notificación real es de la entrega 2; en la entrega 1 se registra en el log).
Las solicitudes de devolución pendientes de ese evento DEBEN cerrarse.

#### Scenario: Cancelación con ventas
- **WHEN** se cancela un evento con 4 entradas vendidas
- **THEN** las 4 pasan a CANCELADA y se pide un reintegro por cada una

#### Scenario: Cancelación repetida
- **WHEN** se procesa dos veces la cancelación del mismo evento
- **THEN** la segunda vez no se reintegra nada de nuevo
