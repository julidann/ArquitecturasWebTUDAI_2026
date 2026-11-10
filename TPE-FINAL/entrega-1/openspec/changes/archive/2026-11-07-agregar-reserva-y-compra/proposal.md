# Agregar reserva temporal, compra y control de doble venta

## Why
Es el núcleo del negocio y el punto más crítico de la consigna: comprar entradas con reserva temporal de
10 minutos, emitir QR y garantizar que una entrada nunca se venda dos veces, aun con muchos clientes
simultáneos, varias instancias del servicio o reintentos por cortes de conexión (funcionalidades 4 y 5).

## What Changes
- Nuevo `ms-reservas` (`reservas_db`): inventario por evento, reservas PENDIENTE/CONFIRMADA/LIBERADA/VENCIDA,
  job de vencimiento, límite de entradas por cliente.
- Nuevo `ms-pagos` (`pagos_db`): pasarela simulada con cobros y reintegros idempotentes.
- Nuevo `ms-compras` (`compras_db`): orquesta reserva → pago → confirmación → emisión de entradas con QR.
- `Idempotency-Key` obligatoria en `POST /compras`.

## Impact
- Specs afectadas: `reservas` (nueva), `compras` (nueva).
- Código: `ms-reservas`, `ms-pagos`, `ms-compras`, rutas en `api-gateway`.
