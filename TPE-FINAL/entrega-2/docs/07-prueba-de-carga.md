# 07 - Prueba de carga

Este documento es el informe de la prueba de carga que pide la entrega 2. Lo que se quería ver es:

1. Cuánto aguanta el sistema el escenario más pesado (mucha gente comprando el mismo evento a la vez).
2. Que **no se vendan más lugares de los que hay**, aun con mucha concurrencia y con 2 instancias de ms-reservas.
3. Qué pasa cuando la pasarela de pagos falla (resiliencia).

## 1. Herramienta y escenario

Usamos **Apache JMeter 5.6.3**. Todo lo necesario está en `carga/`:

| Archivo | Para qué |
|---|---|
| `preparar.sh [clientes] [capacidad]` | Crea por la API (con el JWT del organizador) un lugar con un sector "Campo" no numerado, un evento publicado con límite de 20 entradas por cliente, y registra los clientes `carga-<prefijo>-N@mail.com`. Guarda los ids en `ultima-preparacion.txt`. |
| `compra.jmx` | El plan de JMeter. |
| `ejecutar.sh [usuarios] [iteraciones] [rampa] [gw]` | Corre el plan sin interfaz (`jmeter -n`), con el JMeter instalado (`JMETER=...`) o con la imagen de docker `justb4/jmeter`. Deja el `.jtl` y el reporte HTML en `carga/resultados/<fecha>/`. |
| `analizar.py <archivo.jtl>` | Arma la tabla de tiempos por paso (promedio, percentiles, códigos). |

Cada usuario virtual (un hilo de JMeter) hace:

1. **Login** una sola vez (`POST /api/auth/login`) y guarda el JWT.
2. Por cada iteración, con `Authorization: Bearer <token>`:
   - `GET /api/eventos?texto=...` (buscar)
   - `GET /api/eventos/{id}` (detalle)
   - `GET /api/eventos/{id}/disponibilidad`
   - espera 300 ms (tiempo de "pensar")
   - `POST /api/reservas` pidiendo 2 entradas del Campo. Se aceptan **201** y **409** (agotado), porque cuando se acaba el cupo el 409 es la respuesta correcta.
   - si reservó, `POST /api/compras` con tarjeta de prueba aprobada y una `Idempotency-Key` nueva.

Todo pasa por el **gateway**, así que cada request valida el JWT y recorre la misma cadena que un usuario real.

### Cómo se corre

```bash
docker compose up -d --build              # sistema completo (ms-reservas con 2 instancias)
./carga/preparar.sh 200 1500              # 200 clientes, 1500 lugares
./carga/ejecutar.sh 200 5 30              # 200 usuarios, 5 iteraciones, rampa de 30 s
python3 carga/analizar.py carga/resultados/<fecha>/resultados.jtl
```

Para la corrida con fallas de la pasarela se levanta ms-pagos con `PASARELA_TASA_FALLOS=0.2`:

```bash
PASARELA_TASA_FALLOS=0.2 docker compose up -d ms-pagos
```

### Máquina

Todo (los 10 servicios, ms-reservas ×2, PostgreSQL, MongoDB, Redis, RabbitMQ **y** JMeter) corrió en la misma
máquina: Linux, **4 CPU** y **15 GB de RAM**. Los números son entonces bastante pesimistas: JMeter le saca CPU
a los servicios y no hay red real en el medio.

## 2. Resultados

Las tres corridas usan 200 usuarios, 5 iteraciones y rampa de 30 s, sobre un evento con **1500 lugares**: son
1000 pedidos de 2 entradas (2000 entradas pedidas) para 1500 lugares, así que se espera vender todo y que 250
pedidos reciban 409.

### Corrida A: todo funcionando

`4950 requests en 37 s (135 req/s), 0 errores`

| Paso | Requests | Prom. (ms) | p50 | p90 | p95 | p99 | Máx. | Códigos |
|---|---|---|---|---|---|---|---|---|
| login | 200 | 906 | 766 | 1899 | 2157 | 2519 | 2553 | 200: 200 |
| buscar eventos | 1000 | 23 | 9 | 60 | 90 | 213 | 363 | 200: 1000 |
| detalle del evento | 1000 | 19 | 9 | 44 | 70 | 162 | 227 | 200: 1000 |
| disponibilidad | 1000 | 34 | 15 | 83 | 125 | 327 | 460 | 200: 1000 |
| reservar 2 entradas | 1000 | 55 | 29 | 126 | 177 | 397 | 784 | 201: 750, 409: 250 |
| comprar | 750 | 584 | 557 | 797 | 921 | 1162 | 1360 | 201: 750 |

Control de consistencia al terminar (consultando la API y las bases):

- Disponibilidad del Campo: **0 de 1500**. Entradas emitidas: **1500**. Compras APROBADAS: 750 = pagos APROBADOS: 750.
- No hubo ninguna venta de más: los 250 pedidos que llegaron tarde recibieron 409.

### Corrida B: pasarela con 20 % de fallas (configuración inicial del circuit breaker)

`4950 requests en 36 s, 514 errores (todos en "comprar", 503)`

| Paso | Requests | Prom. (ms) | p95 | Códigos |
|---|---|---|---|---|
| reservar 2 entradas | 1000 | 24 | 75 | 201: 750, 409: 250 |
| comprar | 750 | 707 | 1232 | 201: 236, **503: 514** |

Con la configuración que teníamos al principio (ventana de 10 llamadas, abrir con 50 % de fallas) el circuit
breaker de ms-compras → ms-pagos **se abría solo por rachas de mala suerte**: con 20 % de fallas, en una ventana
de 10 es fácil que caigan 5. Una vez abierto rechazaba todas las compras durante el tiempo de espera, aunque la
pasarela anduviera el 80 % de las veces. Además los reintentos contaban la excepción "circuito abierto" como
otra falla.

Lo bueno: **la consistencia se mantuvo**. Las 514 compras quedaron en estado ERROR y el job de recuperación
(`RecuperacionComprasJob`) las cerró como RECHAZADAS sin cobrar nada y liberó las reservas: al final, compras
APROBADAS 236 = pagos APROBADOS 236, sin cobros huérfanos.

### Corrida C: pasarela con 20 % de fallas (circuit breaker ajustado)

Cambios después de la corrida B (ver `ms-compras/src/main/resources/application.yml`):

- Ventana de **50 llamadas**, mínimo **20** antes de calcular, umbral 50 %, 10 s abierto, 10 llamadas de prueba.
- Nueva `CircuitoAbiertoException`: el retry la **ignora** (no tiene sentido reintentar contra un circuito abierto).

`4950 requests en 36 s, 3 errores (0,4 %)`

| Paso | Requests | Prom. (ms) | p50 | p90 | p95 | p99 | Máx. | Códigos |
|---|---|---|---|---|---|---|---|---|
| login | 200 | 358 | 243 | 761 | 1054 | 1408 | 1519 | 200: 200 |
| buscar eventos | 1000 | 25 | 9 | 54 | 85 | 413 | 543 | 200: 1000 |
| detalle del evento | 1000 | 18 | 8 | 38 | 64 | 161 | 359 | 200: 1000 |
| disponibilidad | 1000 | 18 | 10 | 39 | 60 | 115 | 156 | 200: 1000 |
| reservar 2 entradas | 1000 | 29 | 17 | 68 | 95 | 145 | 193 | 201: 750, 409: 250 |
| comprar | 750 | 718 | 600 | 1162 | 1375 | 1903 | 2408 | 201: 747, 503: 3 |

- La pasarela simuló **184 fallas**; casi todas las absorbieron los **reintentos** (con la misma Idempotency-Key,
  así que nunca se cobró dos veces).
- Solo 3 compras fallaron las 3 veces seguidas y devolvieron 503. El job las cerró como RECHAZADAS y liberó sus
  6 lugares.
- Estado final: 747 compras APROBADAS = 747 pagos APROBADOS, 3 RECHAZADAS, 0 cobros sin compra.

## 3. Conclusiones

- **No hay sobreventa**: en todas las corridas las entradas emitidas nunca superaron la capacidad, con 2
  instancias de ms-reservas atendiendo a la vez. Esto confirma lo que prueban `NoDobleVenta*Test` y
  `NoDobleVentaIT` (ver [05](05-concurrencia-y-pruebas.md)).
- Las lecturas (búsqueda, detalle, disponibilidad) responden en **menos de 100 ms en el p95**. El detalle y la
  búsqueda salen del cache de Redis.
- **Reservar** es rápido (p95 < 200 ms) aunque es el paso que bloquea filas en PostgreSQL.
- **Comprar** es lo más lento (≈ 600 ms de mediana) porque incluye la saga completa y **los 300 ms que tarda a
  propósito la pasarela simulada**. Sin esa demora estaría cerca de los 300 ms.
- El **login** es caro (hasta 2,5 s en la corrida A, con la JVM recién arrancada) por el costo de BCrypt.
  Es intencional (hace más difícil adivinar claves) y en el uso real cada usuario se loguea una vez y reutiliza
  el JWT durante 8 horas. En las corridas B y C, ya con la JVM "caliente", bajó a ~1 s de p95.
- **Lección de la corrida B**: un circuit breaker mal dimensionado puede hacer más daño que la falla misma.
  Con una ventana chica se abre por ruido estadístico. Lo dejamos con ventana de 50 y mínimo de 20 llamadas.
- Aun cuando las cosas fallan, el **outbox + job de recuperación + idempotencia** dejan el sistema consistente:
  ningún pago sin compra, ninguna compra aprobada sin pago, ningún lugar bloqueado para siempre.
- En una máquina de 4 CPU con todo adentro sostuvimos ~135 req/s. El cuello de botella fue la CPU (BCrypt en el
  login y JMeter en la misma máquina). Para eventos más grandes se puede escalar horizontalmente ms-reservas y
  ms-compras (ya probamos 2 instancias de reservas) y, sobre todo, usar la **fila virtual** para limitar
  cuántos compran a la vez.

## 4. Primer intento (descartado)

La primera corrida completa (`20261118-195512`) dio 93 errores, pero no eran del sistema sino del plan de
JMeter: el CSV con los usuarios avanzaba en cada iteración y varios hilos terminaban usando el mismo cliente, que
superaba el límite de 20 entradas (422). Lo arreglamos armando el email con el número de hilo
(`carga-${prefijo}-${__threadNum}`). También hubo algunos 504 en el login con la JVM fría, que no se repitieron.
