# Diseño: entrega 2

El detalle completo está en `docs/02-arquitectura.md` (arquitectura y consistencia) y
`docs/06-decisiones-tecnologicas.md` (justificación de tecnologías y patrones). Acá quedan las decisiones
principales que se discutieron en el grupo.

## Seguridad
- **ms-usuarios emite el JWT (HS256) y el gateway lo valida** con Spring Security (OAuth2 Resource Server).
  Los servicios no validan el token: confían en `X-Usuario-Id` / `X-Rol`, que el gateway **siempre pisa** con
  los datos del token. Así los servicios de la entrega 1 no cambian su forma de recibir el usuario y no se
  puede falsificar la identidad. Los servicios no exponen puertos fuera de la red de docker.
- La **matriz de permisos** está en un solo lugar (configuración de seguridad del gateway) y la prueba
  `SeguridadEndpointsTest` la recorre entera.

## Fila virtual
- **Redis** con un sorted set por evento (score = orden de llegada con `INCR`) para que la posición sea O(log n)
  y los ingresos simultáneos no se pisen. La tanda la habilita un job con **lock en Redis** (`SET NX PX`) para
  que con varias instancias se habilite una sola vez.
- El **turno es un token firmado (HMAC)** con evento, usuario y vencimiento: ms-reservas lo valida sin llamar a
  ms-fila (no agrega una dependencia síncrona en el camino crítico).

## Consistencia
- **Saga orquestada por ms-compras** (reserva → cobro → confirmar reserva → emitir entradas) con
  compensaciones: si se cobró y no se pudo confirmar, se reintegra. Las compras que quedan a medias pasan a
  ERROR y las termina `RecuperacionComprasJob`.
- **Transactional Outbox** en ms-eventos y ms-compras: el mensaje se guarda en la misma transacción que el
  cambio de estado y un job lo publica en RabbitMQ. Garantiza "al menos una vez".
- **Consumidores idempotentes**: cada consumidor usa un id natural (compraId, `TIPO:ref:usuario`) como clave
  única en MongoDB, así un mensaje repetido no duplica nada.
- Las vistas de lectura de ms-recomendaciones (catálogo, historial) son **eventualmente consistentes**.

## Resiliencia
- Resilience4j en ms-compras → ms-pagos: timeout, hasta 3 intentos con la misma `Idempotency-Key` y circuit
  breaker (ventana 50, mínimo 20 llamadas, 50 %). La ventana se agrandó después de la prueba de carga (ver
  `docs/07`): con 10 llamadas se abría por rachas.
- ms-notificaciones: reintentos + circuit breaker contra ms-usuarios / ms-recomendaciones y contra el
  proveedor de email; si falla, la notificación queda PENDIENTE con backoff.

## Cache
- Cache-aside con Spring Cache + Redis en ms-eventos: detalle (se invalida al modificar) y búsquedas (TTL
  corto). La disponibilidad no se cachea porque tiene que ser exacta.

## Trazabilidad
- El gateway genera `X-Correlation-Id`; cada servicio lo pone en el MDC (aparece en todos los logs) y lo
  propaga en REST y en los headers de AMQP. Las operaciones críticas se publican al exchange
  `ticketya.bitacora` y ms-bitacora las guarda en MongoDB. Publicar en la bitácora nunca hace fallar la
  operación de negocio.

## Por qué MongoDB en tres servicios
- Preferencias, historial y notificaciones son documentos con listas y campos variables que no se cruzan con
  otras tablas; la bitácora es de escritura masiva y consultas por filtros. Las aggregations de MongoDB
  resuelven "también compraron" sin joins.
