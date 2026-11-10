# Diseño: reserva, compra y no doble venta

## Contexto
Varias instancias de `ms-reservas` pueden estar atendiendo a la vez la apertura de venta de un recital.
Cualquier control en memoria (synchronized, locks de Java) solo sirve dentro de un proceso, así que el control
tiene que estar en el único recurso compartido: la base de datos.

## Decisiones
1. **UPDATE condicional (compare-and-set) por asiento**:
   `UPDATE asiento_disponibilidad SET estado='RESERVADO', reserva_id=? WHERE evento_id=? AND asiento_id IN (...) AND estado='DISPONIBLE'`.
   Se compara la cantidad de filas afectadas con la pedida; si no coincide se hace rollback de toda la
   reserva (todo o nada). La base serializa las escrituras sobre la misma fila: la segunda transacción espera
   el lock, re-evalúa el `WHERE` y ya no encuentra el asiento disponible.
2. **Cupos de zonas no numeradas** con `UPDATE ... SET disponibles = disponibles - n WHERE disponibles >= n`.
   Nunca puede quedar negativo.
3. **Transiciones de la reserva también condicionales** (`... WHERE estado='PENDIENTE' AND expira_en > now`),
   así el job de vencimiento y la confirmación del pago no pueden ganar los dos.
4. **Restricciones de unicidad como red de seguridad**: `(evento_id, asiento_id)` en el inventario,
   `reserva_id` único en `compra`, `idempotency_key` único en `compra`/`pago`/`reintegro`, y
   `entrada.lugar_activo` único (vale `evento-asiento` mientras la entrada es válida).
5. **Idempotencia de punta a punta**: el cliente manda `Idempotency-Key`; ms-compras usa `compra-{id}` como
   clave hacia ms-pagos, por lo que reintentar nunca cobra dos veces.
6. **Sin transacción distribuida**: ms-compras guarda cada paso en una transacción local corta y, si el pago
   se aprobó pero la reserva no se pudo confirmar, compensa con un reintegro. Si un servicio no responde, la
   compra queda en ERROR y se reintenta con la misma clave. (La estrategia completa de consistencia se
   documenta en la entrega 2.)
7. **Datos copiados a ms-reservas** (precio, fecha, límite) para no llamar a ms-eventos en cada reserva.

## Alternativas consideradas
- `SELECT ... FOR UPDATE` (bloqueo pesimista): también funciona, pero son dos round-trips y hay que cuidar el
  orden de bloqueo para evitar deadlocks con varios asientos. El UPDATE condicional hace lo mismo en una sola
  sentencia.
- `@Version` (bloqueo optimista) leyendo y guardando la entidad: funciona pero con 100 clientes por el mismo
  asiento genera muchas excepciones y reintentos. Igual dejamos `@Version` en las entidades.
- Redis con `SETNX` para la reserva temporal: lo evaluamos para la entrega 2 (fila virtual / reservas con TTL).

## Riesgos
- Un mismo cliente haciendo dos reservas en paralelo podría pasarse del límite por cliente (la verificación
  no es atómica). Lo aceptamos para la entrega 1: no genera doble venta, solo un cliente con alguna entrada de más.
