# Agregar devoluciones y reintegros por cancelación

## Why
Funcionalidad 7: el cliente puede devolver una entrada hasta 72 horas antes y, si el evento se cancela, todas
las entradas se reintegran.

## What Changes
- Solicitudes de devolución con aprobación de un administrador.
- Al aprobar: liberación del lugar en ms-reservas + reintegro en ms-pagos.
- Endpoint interno en ms-compras para procesar la cancelación de un evento.

## Impact
- Specs afectadas: `devoluciones` (nueva).
- Código: `ms-compras` (`DevolucionService`, `CancelacionEventoService`), `ms-reservas` (liberaciones).
