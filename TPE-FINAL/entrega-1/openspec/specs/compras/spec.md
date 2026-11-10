# compras Specification

## Purpose
Pago de una reserva, emisión de entradas con QR y registro de la compra (funcionalidades 4 y 5).
Microservicios: `ms-compras` (orquesta), `ms-pagos` (pasarela simulada) y `ms-reservas`.

## Requirements

### Requirement: Pago de una reserva
El sistema DEBE permitir al cliente pagar su reserva pendiente indicando el medio de pago. Si el pago es
aprobado DEBE confirmar la reserva y emitir una entrada por cada lugar, cada una con un código QR único.
DEBE registrarse el detalle de la compra (evento, sector, cantidad, monto) y el medio de pago (sin guardar
el número completo de la tarjeta).

#### Scenario: Pago aprobado
- **WHEN** el cliente paga una reserva pendiente de 2 asientos con una tarjeta válida
- **THEN** la compra queda APROBADA, los asientos pasan a VENDIDO y se emiten 2 entradas VALIDAS con QR distinto

#### Scenario: Pago rechazado
- **WHEN** la pasarela rechaza el pago
- **THEN** la compra queda RECHAZADA (402), la reserva se libera y no se emiten entradas

#### Scenario: Reserva de otro cliente
- **WHEN** un cliente intenta pagar una reserva que no es suya
- **THEN** el sistema responde 403 y no se cobra nada

#### Scenario: Reserva vencida
- **WHEN** el cliente intenta pagar una reserva vencida o liberada
- **THEN** el sistema responde 409 y no se cobra nada

### Requirement: Idempotencia de la compra
`POST /compras` DEBE exigir una `Idempotency-Key`. Un reintento con la misma clave NO DEBE volver a cobrar
ni emitir entradas: DEBE devolver el resultado original. Una reserva DEBE tener como máximo una compra.

#### Scenario: Reintento por corte de conexión
- **WHEN** el cliente reenvía la misma compra con la misma Idempotency-Key
- **THEN** recibe 200 con la misma compra y la pasarela fue invocada una sola vez

#### Scenario: Clave reutilizada para otra reserva
- **WHEN** se usa una Idempotency-Key ya usada con otra reserva
- **THEN** el sistema responde 422

#### Scenario: Dos claves para la misma reserva
- **WHEN** se intenta pagar con otra clave una reserva que ya tiene una compra
- **THEN** el sistema responde 409

### Requirement: Fallos parciales en la compra
Si un servicio no responde durante la compra, la compra DEBE quedar en ERROR y poder reintentarse con la
misma Idempotency-Key. Si el pago se aprobó pero la reserva ya no se puede confirmar (venció o el evento se
canceló), el sistema DEBE reintegrar el dinero (compensación).

#### Scenario: Pasarela caída
- **WHEN** ms-pagos no responde
- **THEN** la compra queda en ERROR (503) y un reintento con la misma clave la completa sin cobrar dos veces

#### Scenario: Reserva vencida después de cobrar
- **WHEN** el pago se aprueba pero la reserva venció mientras se procesaba
- **THEN** se pide el reintegro total a ms-pagos, la compra queda RECHAZADA y no se emiten entradas

### Requirement: Pasarela de pagos simulada
`ms-pagos` DEBE simular la pasarela: aprobar o rechazar según el medio de pago, registrar cada intento y
permitir reintegros totales o parciales que nunca superen lo cobrado. Cobros y reintegros DEBEN ser
idempotentes por clave.

#### Scenario: Tarjeta de prueba rechazada
- **WHEN** se paga con una tarjeta terminada en 0002
- **THEN** el pago queda RECHAZADO con motivo "Fondos insuficientes"

#### Scenario: Reintegro mayor a lo cobrado
- **WHEN** la suma de reintegros superaría el monto cobrado
- **THEN** el sistema responde 409

### Requirement: Consulta de compras y entradas
El cliente DEBE poder ver sus compras, sus entradas y la imagen del QR de cada entrada.

#### Scenario: Ver QR
- **WHEN** el cliente pide `GET /entradas/{id}/qr` de una entrada suya
- **THEN** recibe una imagen PNG con el código QR
