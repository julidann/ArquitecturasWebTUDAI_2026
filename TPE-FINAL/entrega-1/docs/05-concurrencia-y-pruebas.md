# No doble venta, idempotencia y pruebas

## 1. El problema

En la apertura de venta de un recital, miles de clientes eligen asientos en los mismos segundos y puede haber
varias instancias de `ms-reservas` atendiendo. El sistema tiene que garantizar que:

1. un asiento numerado (o el último lugar de una zona) **no quede tomado por dos clientes**;
2. el **reintento** de un pago (por un corte de conexión) **no cobre dos veces** ni emita entradas duplicadas;
3. el **vencimiento** de una reserva y la **confirmación** de su pago no se pisen.

Cualquier solución en memoria (`synchronized`, `ReentrantLock`, un `Map` de asientos tomados) solo funciona dentro
de un proceso: con dos instancias del servicio deja de servir. Por eso el control está en el único recurso que
comparten todas las instancias, **la base de datos**.

## 2. Mecanismo elegido

### 2.1 UPDATE condicional (compare-and-set) sobre cada asiento

Cada asiento de cada evento es una fila de `asiento_disponibilidad` con su `estado`. Reservar es:

```sql
UPDATE asiento_disponibilidad
   SET estado = 'RESERVADO', reserva_id = :reservaId, version = version + 1
 WHERE evento_id = :eventoId AND sector_id = :sectorId
   AND asiento_id IN (:asientos)
   AND estado = 'DISPONIBLE';
```

y el service compara las **filas afectadas** con la cantidad de asientos pedidos
(`AsientoDisponibilidadRepository.reservar` y `ReservaService.crear`):

- si coinciden, la reserva sigue;
- si no (alguno ya no estaba disponible) se lanza `LugarNoDisponibleException` y **se hace rollback de toda la
  transacción**: ni la reserva ni los asientos que sí se habían tomado quedan guardados (todo o nada).

¿Por qué funciona con concurrencia? La base toma un lock de escritura sobre cada fila que actualiza. Si dos
transacciones quieren el mismo asiento, la segunda espera a que la primera termine; cuando obtiene el lock,
PostgreSQL **vuelve a evaluar el `WHERE`** sobre la versión confirmada de la fila (en READ COMMITTED), ve
`estado = 'RESERVADO'` y no la actualiza. Resultado: 0 filas afectadas → rollback → 409 para ese cliente.

### 2.2 Cupo de zonas no numeradas

```sql
UPDATE sector_disponibilidad
   SET disponibles = disponibles - :cantidad, version = version + 1
 WHERE evento_id = :eventoId AND sector_id = :sectorId AND disponibles >= :cantidad;
```

Mismo principio: si no alcanza el cupo, 0 filas afectadas. `disponibles` **nunca puede quedar negativo**.

### 2.3 Transiciones de la reserva

Confirmar (pago aprobado) y vencer (job cada 30 s) también son UPDATEs condicionales:

```sql
-- confirmar
UPDATE reserva SET estado='CONFIRMADA' WHERE id=:id AND estado='PENDIENTE' AND expira_en > :ahora;
-- vencer / liberar
UPDATE reserva SET estado='VENCIDA'    WHERE id=:id AND estado='PENDIENTE';
```

Si corren a la vez, solo una afecta la fila: o la reserva se confirma y los asientos pasan a VENDIDO, o vence y
vuelven a DISPONIBLE. Nunca las dos. Por la misma razón el job puede correr en todas las instancias al mismo tiempo.

### 2.4 Restricciones de unicidad (red de seguridad)

| Restricción | Qué evita |
|---|---|
| `asiento_disponibilidad (evento_id, asiento_id)` UNIQUE | Dos filas de inventario para el mismo asiento (y por lo tanto dos estados). |
| `compra.reserva_id` UNIQUE | Cobrar dos veces la misma reserva con distintas claves. |
| `compra.idempotency_key`, `pago.idempotency_key`, `reintegro.idempotency_key`, `liberacion.clave` UNIQUE | Procesar dos veces el mismo pedido aunque lleguen exactamente a la vez. |
| `entrada.lugar_activo` UNIQUE (vale `evento-asiento` mientras la entrada está VALIDA, `null` si se devuelve o cancela) | Que existan dos entradas válidas para el mismo asiento aunque fallara todo lo anterior. |

### 2.5 Idempotencia de la compra

- El cliente manda `Idempotency-Key` (un UUID generado por la app) en `POST /compras`.
- `ms-compras` guarda la compra con esa clave antes de cobrar. Si llega otro request con la misma clave:
  - compra APROBADA o RECHAZADA → devuelve el resultado original (200);
  - compra PENDIENTE (se está procesando) → 409 "se está procesando";
  - compra en ERROR (falló otro servicio) → se reintenta el procesamiento.
- Hacia `ms-pagos` siempre se usa la clave `compra-{id}`: aunque `ms-compras` reintente el cobro, la pasarela
  devuelve el pago original y **no cobra dos veces**.
- Las devoluciones usan `devolucion-{id}` tanto para liberar el lugar como para el reintegro, y las cancelaciones
  `cancelacion-entrada-{id}`.

### 2.6 Alternativas consideradas

| Alternativa | Por qué no |
|---|---|
| `synchronized` / locks en memoria | No funciona con más de una instancia. |
| `SELECT ... FOR UPDATE` (pesimista) | Funciona, pero son dos round-trips por reserva y con varios asientos hay que ordenar los locks para no tener deadlocks. El UPDATE condicional hace lo mismo en una sentencia. |
| Solo `@Version` (optimista) leyendo y guardando la entidad | Correcto, pero con 100 clientes por el mismo asiento genera 99 excepciones de concurrencia que hay que manejar. Igual dejamos `@Version` en las entidades y el handler traduce esos errores a 409. |
| Lock distribuido en Redis | Agrega una pieza más que puede fallar y hay que manejar expiraciones; lo vamos a evaluar en la entrega 2 para la fila virtual. |

## 3. Cómo se probó

### 3.1 Prueba de concurrencia automática

`ms-reservas/src/test/java/com/ticketya/reservas/concurrencia/NoDobleVentaTestBase.java`

Lanza 100 hilos (un cliente distinto cada uno) que arrancan **todos a la vez** con un `CountDownLatch` y llaman
al `ReservaService` real contra una base real (sin mocks, porque el mecanismo está justamente en la base).
Se ejecuta dos veces:

- `NoDobleVentaH2Test`: con H2 en memoria (corre siempre, no necesita Docker).
- `NoDobleVentaPostgresTest`: con **PostgreSQL 16 en un contenedor** (Testcontainers). Se saltea solo si no hay
  Docker.

| Test | Escenario | Resultado esperado |
|---|---|---|
| `cienClientesQuierenElMismoAsientoYSoloUnoLoConsigue` | 100 clientes, mismo asiento | 1 éxito, 99 × 409; asiento RESERVADO; sector con 9 de 10 disponibles |
| `cienClientesQuierenElUltimoLugarDeUnaZonaYSoloUnoLoConsigue` | 100 clientes, zona con 1 lugar | 1 éxito; 0 disponibles |
| `laZonaNuncaSeSobrevende` | 100 clientes × 2 lugares, zona de 25 | exactamente 12 éxitos; queda 1 libre |
| `pedidosConAsientosSuperpuestosSonTodoONada` | mitad pide A1-A3, mitad A3-A5 | 1 éxito; exactamente 3 asientos reservados (sin "huérfanos") |
| `unaReservaVencidaLiberaElAsientoYNoSePuedeConfirmar` | se adelanta el reloj 11 min | reserva VENCIDA, no se puede confirmar, otro cliente puede reservar |
| `confirmarYVencerALaVezNuncaDejaElAsientoVendidoYLibre` | confirmación y vencimiento en paralelo | estado de reserva y asiento coherentes |
| `dosInstanciasDelServicioCompitenPorElMismoAsientoYSoloSeVendeUnaVez` (solo PostgreSQL) | se levanta una **segunda instancia completa** de ms-reservas sobre la misma base y los 100 clientes se reparten entre las dos | 1 éxito, 99 × 409 |

Resultado de la última corrida (`mvn test` en `ms-reservas`):

```
Tests run: 6,  Failures: 0, Errors: 0 -- NoDobleVentaH2Test
Tests run: 7,  Failures: 0, Errors: 0 -- NoDobleVentaPostgresTest
Tests run: 9,  Failures: 0, Errors: 0 -- ReservaServiceTest
```

> Nota: con Docker Engine 29+ la librería docker-java necesita `api.version=1.44`
> (está en `ms-reservas/src/test/resources/docker-java.properties`).

### 3.2 Prueba contra el sistema levantado

`scripts/prueba-doble-venta.sh` manda N requests simultáneos por el **gateway** pidiendo el mismo asiento:

```
$ ./scripts/prueba-doble-venta.sh 2 2 15 80
Resultados de 80 pedidos simultaneos:
      1 201
     79 409
OK: el asiento se reservo una sola vez
```

### 3.3 Idempotencia

- `CompraServiceTest.reintentarConLaMismaClaveDevuelveLoMismoSinCobrarDeNuevo`: la pasarela se invoca una sola vez.
- `CompraServiceTest.siLaPasarelaNoRespondeLaCompraQuedaEnErrorYSePuedeReintentar`: ERROR → reintento → APROBADA
  con la misma clave hacia la pasarela.
- `CompraServiceTest.laBaseRechazaDosEntradasValidasParaElMismoAsiento`: la restricción `lugar_activo` frena una
  entrada duplicada aunque otro servicio fallara.
- `PagoServiceTest.reintentarConLaMismaClaveNoCobraDosVeces` y `elReintegroEsIdempotente`.

## 4. Resumen de pruebas por servicio

Se corren todas desde la raíz con `mvn test` (o en cada proyecto con `./mvnw test`).

| Proyecto | Clase | Tipo | Qué cubre |
|---|---|---|---|
| ms-eventos | `EventoServiceTest` (14) | Unitaria (Mockito) | creación, precios, permisos, publicación (con y sin precios, falla de ms-reservas), suspensión, cancelación |
| ms-eventos | `BusquedaServiceTest` (10) | Con base H2 | cada filtro, combinaciones, orden por fecha/precio/relevancia, tildes, paginado, rango inválido |
| ms-reservas | `ReservaServiceTest` (9) | Unitaria | monto y vencimiento, límite por cliente, validaciones, idempotencia de confirmar/liberar |
| ms-reservas | `NoDobleVenta*Test` (6 + 7) | Concurrencia, H2 y PostgreSQL | ver 3.1 |
| ms-compras | `CompraServiceTest` (10) | Base H2 + clientes mockeados | aprobada, rechazada, idempotencia, ERROR y reintento, compensación, permisos |
| ms-compras | `DevolucionServiceTest` (9) | Base H2 + clientes mockeados | plazo de 72 hs, permisos, aprobar/rechazar, falla y reintento, cancelación de evento |
| ms-pagos | `PagoServiceTest` (7) | Base H2 | tarjetas de prueba, idempotencia, reintegros parciales y límites |
| api-gateway | `GatewayApplicationTest` (3) | Integración | endpoints internos bloqueados, correlation id |

Además, la colección Postman (`postman/`) recorre el flujo completo con 59 aserciones y se puede correr con
`npx newman run postman/TicketYa.postman_collection.json`.

## 5. Trazabilidad especificación → prueba

| Spec (openspec/specs) | Requisito | Prueba |
|---|---|---|
| reservas | No doble venta | `NoDobleVentaH2Test`, `NoDobleVentaPostgresTest`, `scripts/prueba-doble-venta.sh` |
| reservas | Reserva temporal | `ReservaServiceTest.reservaEnZonaNoNumeradaCalculaMontoYVencimiento`, `pedidosConAsientosSuperpuestosSonTodoONada` |
| reservas | Límite de entradas por cliente | `ReservaServiceTest.noSePuedeSuperarElLimiteDeEntradasPorCliente` |
| reservas | Vencimiento y liberación | `unaReservaVencidaLiberaElAsientoYNoSePuedeConfirmar`, `liberarDosVecesSoloReponeElCupoUnaVez` |
| compras | Pago de una reserva | `CompraServiceTest.pagoAprobado...`, `pagoRechazado...`, `noSePuedePagarLaReservaDeOtroCliente`, `noSePuedePagarUnaReservaVencida` |
| compras | Idempotencia de la compra | `reintentarConLaMismaClave...`, `laMismaClaveNoSePuedeUsarParaOtraReserva`, `noSePuedeComprarDosVecesLaMismaReservaConClavesDistintas` |
| compras | Fallos parciales | `siLaPasarelaNoResponde...`, `siLaReservaVencioMientrasSePagabaSeReintegraElDinero` |
| compras | Pasarela simulada | `PagoServiceTest` |
| devoluciones | Solicitud / resolución / cancelación | `DevolucionServiceTest` |
| gestion-eventos | Publicación, suspensión, cancelación | `EventoServiceTest` |
| busqueda-eventos | Criterios y orden | `BusquedaServiceTest` |
| gestion-lugares | Alta y consulta | colección Postman (carpeta 1) |
