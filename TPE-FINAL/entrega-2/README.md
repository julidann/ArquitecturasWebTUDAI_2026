# TicketYa - TPE Arquitectura Web (TUDAI 2026)

Backend de una ticketera virtual para eventos (recitales, festivales, teatro, deportes y conferencias),
implementado con **microservicios Spring Boot** y un **API Gateway**.

> **Entrega 1 - Core del sistema:** lugares y eventos, publicación, búsqueda por múltiples criterios, compra con
> reserva temporal y emisión de QR, **no doble venta**, devoluciones y cancelación de eventos.
>
> **Entrega 2 - Sistema completo:** fila virtual para eventos de alta demanda, control de acceso con QR,
> preferencias y recomendaciones (MongoDB), notificaciones asincrónicas (RabbitMQ), reportes para organizadores,
> reprogramación de eventos, **JWT con roles**, resiliencia (reintentos, timeouts, circuit breaker), cache
> (Redis), bitácora y log centralizado con correlation id, un contenedor por servicio con un único
> `docker-compose.yml`, tests de integración y prueba de carga.

**Integrantes:** _completar_

## Contenido del repositorio

```
.
├── api-gateway/          Spring Cloud Gateway: valida el JWT y rutea (puerto 8080)
├── ms-usuarios/          Registro, login (emite JWT) y usuarios (8085)                      [E2]
├── ms-eventos/           Lugares, eventos, publicación, búsqueda (con cache Redis) (8081)
├── ms-reservas/          Disponibilidad y reservas temporales, control de doble venta (8082)
├── ms-compras/           Compras (saga), entradas con QR, devoluciones, accesos y reportes (8083)
├── ms-pagos/             Pasarela de pagos simulada (8084)
├── ms-fila/              Fila virtual con Redis (8086)                                       [E2]
├── ms-recomendaciones/   Preferencias y recomendaciones con MongoDB (8087)                   [E2]
├── ms-notificaciones/    Avisos por email (simulado), MongoDB (8088)                          [E2]
├── ms-bitacora/          Log centralizado: recibe por RabbitMQ y guarda en MongoDB (8089)    [E2]
├── tests-integracion/    Tests contra el sistema completo levantado                          [E2]
├── carga/                Prueba de carga con JMeter                                          [E2]
├── openspec/             Especificaciones (spec-driven development con OpenSpec)
├── docs/                 Documentación de diseño
├── postman/              Colección de requests
├── scripts/              Datos de prueba, prueba de doble venta y tests de integración
├── infra/                Script de creación de las bases
├── docker-compose.yml    Todo el sistema: servicios + PostgreSQL, MongoDB, Redis y RabbitMQ
└── pom.xml               POM agregador (solo para compilar/testear todo junto)
```

Cada microservicio es un **proyecto Maven independiente** (con su propio `mvnw` y su `Dockerfile`) que se puede
construir y ejecutar por separado.

## Documentación

| Documento | Contenido |
|---|---|
| [docs/01-features-y-user-stories.md](docs/01-features-y-user-stories.md) | Features e historias de usuario (US-01 a US-29) con criterios de aceptación |
| [docs/02-arquitectura.md](docs/02-arquitectura.md) | Microservicios, diagrama de módulos, API pública, mensajería, diagramas de secuencia, **estrategias de consistencia**, saga y compensaciones, resiliencia, cache, trazabilidad, seguridad y despliegue |
| [docs/03-modelo-de-datos.md](docs/03-modelo-de-datos.md) | Diagrama entidad-relación por microservicio (PostgreSQL), colecciones de MongoDB y claves de Redis |
| [docs/04-api-rest.md](docs/04-api-rest.md) | Autenticación, matriz de permisos por rol, endpoints, códigos de respuesta y ejemplos |
| [docs/05-concurrencia-y-pruebas.md](docs/05-concurrencia-y-pruebas.md) | Mecanismo contra la doble venta, idempotencia, resumen de pruebas y trazabilidad spec → prueba |
| [docs/06-decisiones-tecnologicas.md](docs/06-decisiones-tecnologicas.md) | Justificación de tecnologías y patrones (API Gateway, Circuit Breaker, BFF, Saga, Service Discovery, Outbox, ...) |
| [docs/07-prueba-de-carga.md](docs/07-prueba-de-carga.md) | Informe de la prueba de carga |
| [docs/openapi/](docs/openapi/) | Especificación OpenAPI 3 de cada microservicio (exportada de springdoc) |
| [openspec/](openspec/) | `project.md`, specs por capacidad y changes archivados (propuesta → specs → diseño → tareas) |

## Tecnologías

Java 21 · Spring Boot 3.3 · Spring Data JPA · Spring Data MongoDB · Spring Data Redis · Spring AMQP ·
Spring Cloud Gateway · Spring Security (OAuth2 Resource Server, JWT) · Resilience4j · PostgreSQL 16 · MongoDB 7 ·
Redis 7 · RabbitMQ 3 · H2 · springdoc-openapi · ZXing (QR) · JUnit 5 · Mockito · Testcontainers · WireMock ·
RestAssured · JMeter · Docker / Docker Compose

## Requisitos

- **Docker** con Docker Compose (es la forma recomendada de levantar todo).
- Para correr los tests o los servicios fuera de Docker: JDK 21 (no hace falta Maven, cada proyecto trae `./mvnw`).
- Opcional: `curl` y `jq` para los scripts; Postman o `newman` para la colección; JMeter para la prueba de carga
  (si no está instalado, `carga/ejecutar.sh` usa una imagen de Docker).

## Cómo ejecutar (entrega 2)

### 1. Levantar todo el sistema

```bash
docker compose up -d --build
```

Construye una imagen por microservicio (build multi-etapa: compila con Maven y corre con un JRE 21) y levanta:

| Contenedor | Puerto en el host |
|---|---|
| api-gateway (**único punto de entrada**) | 8080 |
| ms-eventos, ms-reservas (×2), ms-compras, ms-pagos, ms-usuarios, ms-fila, ms-recomendaciones, ms-notificaciones, ms-bitacora | solo red interna |
| PostgreSQL | 5432 |
| MongoDB | 27017 |
| Redis | 6379 |
| RabbitMQ (consola en <http://localhost:15672>, `ticketya`/`ticketya`) | 5672 / 15672 |

La primera vez tarda varios minutos (descarga dependencias y compila). Los servicios arrancan en orden según sus
healthchecks; para ver que esté todo arriba: `docker compose ps` o `curl localhost:8080/actuator/health`.

Variables útiles (se pasan delante de `docker compose up -d`):

| Variable | Default | Para qué |
|---|---|---|
| `RESERVAS_INSTANCIAS` | 2 | Cantidad de instancias de ms-reservas |
| `PASARELA_TASA_FALLOS` | 0 | Probabilidad (0 a 1) de que la pasarela simulada no responda, para probar la resiliencia |
| `PROVEEDOR_TASA_FALLOS` | 0 | Lo mismo para el proveedor de emails de ms-notificaciones |
| `JWT_SECRETO` | clave de desarrollo | Clave HMAC con la que ms-usuarios firma y el gateway valida los JWT |

Para bajar todo: `docker compose down` (con `-v` borra también los datos).

### 2. Usuarios de prueba

ms-usuarios crea estos usuarios al arrancar (el registro público `POST /api/auth/registro` siempre crea clientes):

| Email | Clave | Rol |
|---|---|---|
| admin@ticketya.com | admin123 | ADMIN |
| organizador@ticketya.com | organizador123 | ORGANIZADOR |
| organizador2@ticketya.com | organizador123 | ORGANIZADOR |
| cliente@ticketya.com | cliente123 | CLIENTE |
| cliente2@ticketya.com | cliente123 | CLIENTE |
| acceso@ticketya.com | acceso123 | ACCESO (personal de la puerta) |

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
        -d '{"email": "cliente@ticketya.com", "password": "cliente123"}' | jq -r .token)
curl localhost:8080/api/compras -H "Authorization: Bearer $TOKEN"
```

El gateway valida la firma y el vencimiento del token y les pasa a los servicios el usuario y el rol en los
headers `X-Usuario-Id` / `X-Rol` (los que mande el cliente se pisan, así que no se pueden falsificar).

### 3. Cargar datos de prueba

```bash
./scripts/datos-prueba.sh
```

Hace login con el admin y los organizadores y crea 3 lugares (un estadio en La Plata, un teatro y un
microestadio en Tandil) y 6 eventos: 5 publicados (recital, festival, teatro, conferencia y un show de solo 5
lugares para probar el agotado) y 1 en borrador. También guarda preferencias para `cliente@ticketya.com`.

### 4. Probar

- **Swagger UI unificado:** <http://localhost:8080/swagger-ui.html> (selector arriba a la derecha para cada
  servicio). Con el botón **Authorize** se pega el JWT.
- **Postman:** importar `postman/TicketYa.postman_collection.json` y correr las carpetas en orden con el Runner.
  La carpeta 0 hace login con cada rol y guarda los tokens; el resto usa Bearer con el rol que corresponde.
  Desde consola: `npx newman run postman/TicketYa.postman_collection.json` (124 aserciones).
- **curl**, por ejemplo:

```bash
# buscar recitales en La Plata ordenados por precio (público, sin token)
curl "localhost:8080/api/eventos?tipo=RECITAL&ciudad=La%20Plata&orden=precio"

# reservar dos asientos y pagarlos (con el token del cliente)
curl -X POST localhost:8080/api/reservas -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"eventoId": 1, "sectorId": 2, "asientoIds": [3, 4]}'
curl -X POST localhost:8080/api/compras -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -H "Idempotency-Key: $(uuidgen)" \
     -d '{"reservaId": 1, "medioPago": {"tipo": "TARJETA_CREDITO", "numeroTarjeta": "4509953566233704"}}'
```

**Tarjetas de prueba (pasarela simulada):** terminada en `0002` → rechazada por fondos insuficientes;
terminada en `0069` → tarjeta vencida; cualquier otra → aprobada. `MERCADO_PAGO` siempre se aprueba.

**Eventos de alta demanda:** si un evento se crea con `"altaDemanda": true`, al publicarlo se abre su fila
virtual. El cliente entra con `POST /api/fila/eventos/{id}/ingresar`, consulta `GET .../mi-turno` y, cuando está
HABILITADO, reserva mandando el turno en el header `X-Turno-Fila`.

### Sin Docker para los servicios

También se pueden correr los servicios a mano (por ejemplo para depurar uno). Hace falta tener las bases y el
broker arriba (`docker compose up -d postgres mongo redis rabbitmq`) y en una terminal por servicio:

```bash
cd ms-usuarios && ./mvnw spring-boot:run
cd ms-eventos  && ./mvnw spring-boot:run
...
cd api-gateway && ./mvnw spring-boot:run
```

Los servicios de la entrega 1 siguen teniendo el perfil `h2` (`-Dspring-boot.run.profiles=h2`) para correr sin
PostgreSQL.

## Tests

```bash
mvn test                          # desde la raíz: tests unitarios y de componentes de todos los servicios (396)
cd ms-reservas && ./mvnw test     # o de un proyecto
```

Los que necesitan PostgreSQL, Redis o MongoDB reales los levantan con **Testcontainers** (si no hay Docker se
saltean solos).

- Unitarios de la lógica de negocio de cada servicio.
- **No doble venta:** 100 clientes simultáneos por el mismo asiento / último lugar de una zona, con H2 y con
  PostgreSQL y dos instancias del servicio compitiendo.
- **Seguridad:** `api-gateway/.../SeguridadEndpointsTest` prueba **cada endpoint con cada rol y sin token**
  (256 casos), tokens vencidos, firmados con otra clave y headers falsificados.
- **Resiliencia:** reintentos, timeout y circuit breaker de ms-compras contra una pasarela simulada con WireMock;
  proveedor de emails caído en ms-notificaciones.

**Tests de integración** (con el sistema levantado con `docker compose up -d --build`):

```bash
./scripts/tests-integracion.sh    # 13 tests: compra completa, fila virtual, cancelación, cambio de fecha, roles, doble venta
./scripts/prueba-doble-venta.sh <eventoId> <sectorId> <asientoId> [clientes]
```

**Prueba de carga** (ver el informe en [docs/07-prueba-de-carga.md](docs/07-prueba-de-carga.md)):

```bash
./carga/preparar.sh 200 1500      # crea el evento y 200 clientes
./carga/ejecutar.sh 200 5 30      # 200 usuarios, 5 iteraciones, rampa de 30 s
python3 carga/analizar.py carga/resultados/<fecha>/resultados.jtl
```

Detalle de todas las pruebas en [docs/05-concurrencia-y-pruebas.md](docs/05-concurrencia-y-pruebas.md).

## Configuración

| Propiedad | Servicio | Default | Descripción |
|---|---|---|---|
| `jwt.horas-vigencia` | ms-usuarios | 8 | Duración del token |
| `reservas.minutos-vigencia` | ms-reservas | 10 | Duración de la reserva temporal |
| `reservas.intervalo-vencimiento-ms` | ms-reservas | 30000 | Cada cuánto se liberan reservas vencidas |
| `devoluciones.horas-minimas` | ms-compras | 72 | Hasta cuántas horas antes se puede pedir la devolución |
| `resilience4j.*` | ms-compras, ms-notificaciones | ver `application.yml` | Reintentos, timeouts y circuit breakers |
| `pasarela.demora-ms` | ms-pagos | 300 | Latencia simulada de la pasarela |
| `pasarela.tasa-fallos` | ms-pagos | 0 | Probabilidad de que la pasarela no responda |
| `servicios.*.url` | todos | `http://localhost:808X` | URL de los otros servicios (en docker-compose, el nombre del servicio) |
| `servicios.timeout-ms` | eventos, reservas, compras | 2000-3000 | Timeout de las llamadas entre servicios |

## Alcance

**Entrega 1:** funcionalidades 1, 2, 3, 4, 5, 7 y 9; API REST síncrona; API Gateway; una base relacional por
servicio; mecanismo contra la doble venta probado; pasarela mock; OpenAPI; colección de requests; datos de prueba.

**Entrega 2:** funcionalidades 6 (fila virtual), 8 (control de acceso con QR), 10 (preferencias y
recomendaciones), 11 (notificaciones) y 12 (reportes), más reprogramación de eventos; MongoDB en
ms-recomendaciones, ms-notificaciones y ms-bitacora; mensajería asincrónica con RabbitMQ (outbox, consumidores
idempotentes y colas de error); saga de compra con compensaciones; resiliencia con Resilience4j; cache con Redis;
bitácora y log centralizado con correlation id; JWT con roles; un contenedor por servicio y un único
docker-compose; tests de integración, de acceso por rol y prueba de carga.
