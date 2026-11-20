# Decisiones de tecnologías y patrones (entrega 2)

En este documento justificamos las tecnologías que agregamos en la entrega 2 y los patrones de microservicios que
usamos (o que evaluamos y decidimos no usar). Para cada uno decimos **qué problema resuelve en TicketYa**.

## 1. Tecnologías

| Tecnología | Dónde | Para qué | Alternativas que consideramos |
|---|---|---|---|
| **MongoDB 7** | ms-recomendaciones, ms-notificaciones, ms-bitacora | Datos semiestructurados, de esquema variable o de solo-agregar (ver 1.1) | PostgreSQL con columnas JSONB; Cassandra |
| **Redis 7** | ms-eventos (cache), ms-fila | Cache compartido del catálogo; fila virtual con estructuras atómicas en memoria | Caffeine (cache local); fila en PostgreSQL |
| **RabbitMQ 3.13** | todos los servicios | Mensajes de dominio y bitácora (comunicación asincrónica) | Kafka; llamadas REST "fire and forget" |
| **Spring Security + OAuth2 Resource Server (Nimbus)** | api-gateway | Validar el JWT (firma HS256, emisor, vencimiento) y los roles en el gateway reactivo | Validar el token en cada servicio |
| **JJWT** | ms-usuarios, ms-fila, ms-reservas | Firmar los JWT de login y los turnos de la fila | – |
| **Resilience4j** | ms-compras, ms-notificaciones | Retry, circuit breaker y fallback con anotaciones | Spring Retry (no tiene circuit breaker); Hystrix (discontinuado) |
| **Testcontainers** | tests de ms-eventos, ms-fila, ms-reservas, ms-recomendaciones, ms-notificaciones, ms-bitacora | Probar contra Redis, MongoDB y PostgreSQL reales | Bases embebidas (no se comportan igual) |
| **WireMock** | tests del gateway, ms-compras, ms-notificaciones | Simular otros servicios para probar seguridad y resiliencia con los clientes HTTP reales | Mockito (no prueba timeouts ni reintentos reales) |
| **Docker / docker compose** | despliegue | Un contenedor por servicio y por base; todo con un comando | Kubernetes (excesivo para el TP) |
| **JMeter 5.6** | prueba de carga | Simular cientos de clientes comprando | Gatling, k6 |

### 1.1 ¿Por qué MongoDB y por qué en esos servicios?

La consigna pide al menos un servicio con NoSQL justificando la elección. Elegimos MongoDB para tres servicios
donde el modelo de documentos encaja mejor que el relacional:

**ms-recomendaciones (preferencias + historial + catálogo para recomendar)**
- Las preferencias son **listas de largo variable** (tipos de evento, artistas favoritos). En relacional serían
  dos tablas extra con joins; en Mongo es un documento por cliente que se lee y se reemplaza entero (`PUT`).
- Buscar "clientes con alguno de estos artistas como favorito en esta ciudad" es una consulta con `$in` sobre un
  array indexado (índice *multikey*), sin joins.
- "Quienes compraron este evento también compraron..." es un **aggregation pipeline** (`$match` → `$group` con
  `$addToSet` → `$size` → `$sort`), que Mongo resuelve de forma natural.
- Es un **read model** que se reconstruye a partir de mensajes: no necesita transacciones entre varias
  entidades ni integridad referencial (los datos "maestros" están en ms-eventos y ms-compras).
- Escala horizontalmente (sharding por `usuarioId`) si el historial crece mucho, sin tocar el modelo.

**ms-notificaciones**
- Cada notificación es un documento autocontenido (destino, asunto, cuerpo, estado, intentos, error). Se
  escribe mucho y casi no se relaciona con nada.
- El índice único sobre `clave` da la **idempotencia** del consumidor de mensajes.

**ms-bitacora (log centralizado)**
- Cada operación manda un `detalle` distinto (una compra manda monto y entradas, un ingreso manda puerta y
  entrada, un cobro manda el medio de pago). En relacional habría que usar una columna de texto o una tabla por
  tipo; en Mongo el `detalle` es un subdocumento libre y se puede consultar igual.
- Es de **solo agregar** y se consulta por `correlationId`, operación y fecha (índices simples). Mongo es muy
  eficiente para este tipo de carga (es el uso típico de un "log store").

**¿Por qué no Mongo para el catálogo o las reservas?**
- Las **reservas** necesitan transacciones con varias filas (reserva + asientos) y restricciones de unicidad que
  son la base del mecanismo contra la doble venta. PostgreSQL lo resuelve con UPDATE condicionales y UNIQUE
  (ver [05](05-concurrencia-y-pruebas.md)); en Mongo habría que modelarlo con un documento por evento y
  operaciones atómicas sobre un documento gigante, que se vuelve un cuello de botella.
- El **catálogo** tiene relaciones claras (evento → lugar → sectores → asientos, precios por sector) y búsquedas
  por varios filtros que ya resolvimos bien con JPA en la entrega 1. Para la carga de lecturas usamos cache.
- **Compras y pagos** manejan dinero: preferimos ACID clásico.

### 1.2 ¿Por qué Redis?

- **Cache del catálogo** (ms-eventos): el cache tiene que ser **compartido** entre las instancias de ms-eventos
  (si cada una tuviera su cache local, al invalidar en una las otras seguirían mostrando el dato viejo).
  Redis da TTL por clave y es el soporte estándar de Spring Cache. Si Redis se cae, un `CacheErrorHandler` hace
  que se siga leyendo de PostgreSQL.
- **Fila virtual** (ms-fila): en la apertura de una venta miles de clientes entran a la fila y consultan su
  posición cada pocos segundos. Redis lo resuelve en memoria y con operaciones **atómicas**: `INCR` para el
  número de llegada, *sorted set* para el orden (`ZRANK` = posición, `ZPOPMIN` = sacar la siguiente tanda) y
  `SET NX PX` como lock para que solo una instancia habilite cada tanda. Hacerlo en PostgreSQL pondría el pico
  de tráfico justo sobre la base de reservas.
- La consigna sugiere Redis "para reservas temporales o la fila virtual". Elegimos la **fila** y no las
  reservas: las reservas ya estaban resueltas con consistencia fuerte en PostgreSQL y moverlas a Redis
  obligaría a sincronizar dos fuentes de verdad del inventario.

### 1.3 ¿Por qué RabbitMQ y no Kafka?

- Necesitamos **colas de trabajo** con reintentos y *dead letter queues* (un mensaje que falla 3 veces se aparta
  para revisarlo), ruteo por tipo de mensaje (exchange *topic*) y entrega a varios consumidores. Es exactamente
  el modelo de RabbitMQ y Spring AMQP lo configura en pocas líneas.
- Kafka brilla para streams de muy alto volumen y para *replay* de eventos; para el volumen de un TP y para
  notificaciones es más complejo de operar (particiones, offsets) sin ganar nada que usemos.

## 2. Patrones

| Patrón | ¿Lo usamos? | Dónde / por qué |
|---|---|---|
| **API Gateway** | Sí | `api-gateway` (Spring Cloud Gateway) |
| **Circuit Breaker** | Sí | ms-compras → ms-pagos y ms-eventos; ms-notificaciones → proveedor y ms-recomendaciones |
| **Saga** | Sí (orquestada) | Compra en ms-compras |
| **Database per service** | Sí | Cada servicio con su base |
| **Transactional Outbox** | Sí | ms-eventos y ms-compras |
| **Idempotent consumer / Idempotency key** | Sí | Pagos, compras, todos los consumidores de mensajes |
| **Cache-aside** | Sí | Catálogo en ms-eventos |
| **Retry + Timeout + Fallback** | Sí | Clientes REST con Resilience4j |
| **Correlation ID / log centralizado** | Sí | Gateway + ms-bitacora |
| **BFF (Backend for Frontend)** | No | Ver 2.4 |
| **Service Discovery** | No (DNS de Docker) | Ver 2.5 |

### 2.1 API Gateway

Problema: hay cuatro tipos de clientes (apps de clientes y organizadores, backoffice, lectores) y nueve servicios.
Sin gateway cada cliente tendría que conocer la dirección de cada servicio y cada servicio tendría que validar
el token, loguear y aplicar CORS.

En TicketYa el gateway:
- es el **único punto de entrada** (en compose es el único contenedor con puerto publicado);
- **valida el JWT** y aplica la **matriz de roles** en un solo lugar (`ReglasDeAcceso`);
- **pisa los headers de identidad** (`X-Usuario-Id`, `X-Rol`) con los del token: los servicios confían en ellos
  porque nadie puede llegar a un servicio sin pasar por el gateway;
- **bloquea los endpoints internos** (`/internal/**`, confirmar/liberar reservas) devolviendo 404;
- genera el **correlation id** y loguea cada request;
- expone un **Swagger UI unificado** con la documentación de los 9 servicios.

Elegimos Spring Cloud Gateway (reactivo, sobre Netty) porque aguanta muchas conexiones concurrentes con pocos
hilos, que es lo que pasa en una apertura de venta.

### 2.2 Circuit Breaker

Problema: si la pasarela de pagos se cuelga, cada compra espera el timeout (3 s) y reintenta; con cientos de
clientes eso agota los hilos de ms-compras y termina tirando también lo que no depende de pagos.

Con el circuit breaker, cuando falla la mitad de las últimas 50 llamadas el circuito se **abre** y durante 10 s
las compras fallan al instante (503 con un mensaje claro), sin llamar a la pasarela. Después deja pasar 10
llamadas de prueba (*half-open*) y, si andan, se cierra.

En la prueba de carga ([07](07-prueba-de-carga.md)) aprendimos que **la ventana importa**: con una ventana de 10
llamadas y una pasarela que falla al azar el 20 % de las veces, el circuito se abría por "rachas de mala suerte"
y rechazaba el 68 % de las compras aunque la pasarela andaba bien el 80 % del tiempo. Con ventana de 50 y sin
reintentar cuando el circuito está abierto, el 99,6 % de las compras se aprobaron.

### 2.3 Saga

Problema: una compra toca tres bases (reservas, compras, pagos) y no hay transacción distribuida.

Usamos una **saga orquestada**: `ms-compras` ejecuta los pasos (validar reserva → cobrar → confirmar reserva →
emitir entradas) y, si un paso falla después de que otro ya se hizo, ejecuta la **compensación** (liberar la
reserva, reintegrar el pago). El detalle de pasos y compensaciones está en [02 §6](02-arquitectura.md).

¿Por qué orquestada y no coreografiada (cada servicio reacciona a eventos del anterior)?
- El cliente espera la respuesta de la compra en el momento (aprobada / rechazada): con coreografía habría que
  hacer polling o websockets.
- El flujo y sus compensaciones quedan escritos en un solo lugar (`CompraService`), más fácil de entender, testear
  y de recuperar (el job de recuperación sabe en qué paso quedó cada compra).
- Usamos coreografía (eventos) solo para los efectos secundarios que no deciden el resultado: notificaciones,
  recomendaciones, bitácora.

### 2.4 BFF (Backend for Frontend) — no lo usamos

Un BFF es un backend específico por tipo de cliente (uno para la app móvil, otro para el backoffice) que arma
respuestas a medida combinando varios servicios.

Lo evaluamos y **no lo implementamos** porque:
- No hay frontends en el alcance del TP (la GUI es opcional) y las respuestas de cada servicio ya son las que
  necesitaría una pantalla (ej. la compra devuelve las entradas con su QR; el reporte trae todo calculado).
- Agregaría un servicio más por cada cliente para mantener, sin un problema concreto que resuelva hoy.
- Si en el futuro la app móvil necesitara, por ejemplo, una pantalla "mis entradas" que junte compras, entradas
  y notificaciones en una sola llamada, el lugar para hacerlo sería un BFF detrás del gateway
  (`/api/movil/**`), sin tocar los microservicios.

### 2.5 Service Discovery — no usamos un registro (Eureka/Consul)

Service discovery es que los servicios encuentren a los otros por nombre (y a todas sus instancias) en lugar de
usar IPs fijas.

En TicketYa lo resuelve el **DNS interno de Docker**: cada servicio se llama por su nombre (`http://ms-reservas:8082`)
y, como ms-reservas corre con 2 réplicas, el nombre resuelve a las dos IPs. Las URLs se configuran por variable
de entorno (`SERVICIOS_RESERVAS_URL`, ...), así que en otro entorno (Kubernetes, por ejemplo) se cambia la URL
por la del *Service* y listo.

No agregamos Eureka porque sería un componente más (y un punto de falla más) para resolver algo que la
plataforma ya resuelve. Lo que sí tuvimos que ajustar: el cliente HTTP del gateway (Netty) cacheaba las IPs, y al
reiniciarse un contenedor seguía mandando a la IP vieja; lo configuramos para resolver con la JVM y un TTL de
5 s (`HttpClientConfig`).

### 2.6 Transactional Outbox

Problema: al cancelar un evento hay que (a) guardar el cambio en la base y (b) avisar por RabbitMQ. Si se hace
en ese orden y RabbitMQ está caído, el evento queda cancelado pero nadie se entera; si se hace al revés y la
transacción falla, se avisa de algo que no pasó.

Solución: el mensaje se guarda en la tabla `mensaje_saliente` **en la misma transacción** que el cambio, y un job
lo publica (y marca como publicado). Si RabbitMQ no está, el mensaje espera en la tabla. La contra es que un
mensaje puede salir dos veces (si se publica y se cae antes de marcarlo), por eso todos los consumidores son
idempotentes.

### 2.7 Cache-aside

Ver [02 §8](02-arquitectura.md#8-cache-e2). Qué **no** cacheamos y por qué:

| Dato | ¿Cache? | Por qué |
|---|---|---|
| Detalle del evento, búsquedas, lugares | Sí | Se leen muchísimo (todos los clientes antes de comprar) y cambian poco. |
| Disponibilidad de asientos / cupos | **No** | Cambia en cada reserva. Un dato de hace 10 segundos mostraría como libres asientos ya vendidos y el cliente recibiría un 409 al reservar. Además la consulta ya es barata (por índice). |
| Reservas, compras, entradas | **No** | Son datos del usuario que cambian de estado (y el usuario espera verlos al instante). |
| Posición en la fila | **No** (ya está en Redis) | Es el dato "vivo" de la fila. |
| Reportes | No | Los consulta poco el organizador y tienen que estar al día. |
