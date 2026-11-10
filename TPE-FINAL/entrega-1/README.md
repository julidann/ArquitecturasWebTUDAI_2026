# TicketYa - TPE Arquitectura Web (TUDAI 2026)

Backend de una ticketera virtual para eventos (recitales, festivales, teatro, deportes y conferencias),
implementado con **microservicios Spring Boot** y un **API Gateway**.

> **Entrega 1 - Core del sistema:** lugares y eventos, publicación, búsqueda por múltiples criterios, compra con
> reserva temporal y emisión de QR, **no doble venta**, devoluciones y cancelación de eventos.

**Integrantes:**

* D'Annunzio Julieta
* Mensa Matias
* Parasuco Forturella Santiago
* Sánchez Tomé Luciano 



## Contenido del repositorio

```
.
├── api-gateway/        Spring Cloud Gateway (puerto 8080)
├── ms-eventos/         Lugares, eventos, publicación y búsqueda (8081)
├── ms-reservas/        Disponibilidad y reservas temporales, control de doble venta (8082)
├── ms-compras/         Compras, entradas con QR, devoluciones y cancelaciones (8083)
├── ms-pagos/           Pasarela de pagos simulada (8084)
├── openspec/           Especificaciones (spec-driven development con OpenSpec)
├── docs/               Documentación de diseño
├── postman/            Colección de requests
├── scripts/            Datos de prueba y prueba de doble venta
├── infra/              Script de creación de las bases
├── docker-compose.yml  PostgreSQL para desarrollo
└── pom.xml             POM agregador (solo para compilar/testear todo junto)
```

Cada microservicio es un **proyecto Maven independiente** (con su propio `mvnw`) que se puede construir y
ejecutar por separado.

## Documentación

| Documento | Contenido |
|---|---|
| [docs/01-features-y-user-stories.md](docs/01-features-y-user-stories.md) | Features e historias de usuario de todo el sistema con criterios de aceptación |
| [docs/02-arquitectura.md](docs/02-arquitectura.md) | Microservicios, responsabilidades, API pública, diagrama de módulos y diagrama de secuencia de la compra |
| [docs/03-modelo-de-datos.md](docs/03-modelo-de-datos.md) | Diagrama entidad-relación por microservicio, relaciones entre servicios y entidades solapadas |
| [docs/04-api-rest.md](docs/04-api-rest.md) | Listado de endpoints, códigos de respuesta y ejemplos de request/response |
| [docs/05-concurrencia-y-pruebas.md](docs/05-concurrencia-y-pruebas.md) | Mecanismo contra la doble venta, idempotencia y pruebas |
| [docs/openapi/](docs/openapi/) | Especificación OpenAPI 3 de cada microservicio (exportada de springdoc) |
| [openspec/](openspec/) | `project.md`, specs por capacidad y changes archivados (propuesta → specs → diseño → tareas) |

## Tecnologías

Java 21 · Spring Boot 3.3 · Spring Data JPA · Spring Cloud Gateway · PostgreSQL 16 · H2 · springdoc-openapi ·
ZXing (QR) · JUnit 5 · Mockito · Testcontainers · Docker Compose

## Requisitos

- JDK 21
- Docker (para PostgreSQL). Si no tenés Docker podés usar el perfil `h2` (ver más abajo).
- Opcional: `curl` y `jq` para el script de datos de prueba; Postman o `newman` para la colección.

No hace falta tener Maven instalado: cada proyecto trae su wrapper (`./mvnw`, o `mvnw.cmd` en Windows).

## Cómo ejecutar

### 1. Levantar la base de datos

```bash
docker compose up -d
```

Levanta un PostgreSQL en `localhost:5432` (usuario/clave `ticketya`/`ticketya`) y crea una base por
microservicio: `eventos_db`, `reservas_db`, `compras_db` y `pagos_db`. Las tablas las crea cada servicio al arrancar.

### 2. Levantar los microservicios

En una terminal por servicio (el orden no importa, pero el gateway conviene al final):

```bash
cd ms-eventos  && ./mvnw spring-boot:run
cd ms-reservas && ./mvnw spring-boot:run
cd ms-compras  && ./mvnw spring-boot:run
cd ms-pagos    && ./mvnw spring-boot:run
cd api-gateway && ./mvnw spring-boot:run
```

O compilando los jar:

```bash
mvn clean package -DskipTests          # desde la raíz (o ./mvnw package en cada proyecto)
java -jar ms-eventos/target/ms-eventos-1.0.0.jar
...
```

**Sin Docker:** cada microservicio tiene un perfil `h2` con base en memoria:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

Verificar que esté todo arriba: `curl localhost:8080/actuator/health` (y 8081...8084).

### 3. Cargar datos de prueba

```bash
./scripts/datos-prueba.sh
```

Crea 3 lugares (un estadio en La Plata, un teatro y un microestadio en Tandil) y 6 eventos: 5 publicados
(recital, festival, teatro, conferencia y un show de solo 5 lugares para probar el agotado) y 1 en borrador.

### 4. Probar

- **Swagger UI unificado:** <http://localhost:8080/swagger-ui.html> (selector arriba a la derecha para cada servicio).
  También cada servicio tiene el suyo en `http://localhost:808X/swagger-ui.html`.
- **Postman:** importar `postman/TicketYa.postman_collection.json` y correr las carpetas en orden con el Runner
  (los ids se van guardando en variables). Desde consola: `npx newman run postman/TicketYa.postman_collection.json`.
- **curl**, por ejemplo:

```bash
# buscar recitales en La Plata ordenados por precio
curl "localhost:8080/api/eventos?tipo=RECITAL&ciudad=La%20Plata&orden=precio"

# reservar dos asientos y pagarlos
curl -X POST localhost:8080/api/reservas -H "Content-Type: application/json" -H "X-Usuario-Id: 100" \
     -d '{"eventoId": 1, "sectorId": 2, "asientoIds": [3, 4]}'
curl -X POST localhost:8080/api/compras -H "Content-Type: application/json" -H "X-Usuario-Id: 100" \
     -H "Idempotency-Key: $(uuidgen)" \
     -d '{"reservaId": 1, "medioPago": {"tipo": "TARJETA_CREDITO", "numeroTarjeta": "4509953566233704"}}'
```

**Identificación del usuario (entrega 1):** no hay autenticación. Se usan los headers `X-Usuario-Id` y `X-Rol`
(`CLIENTE` por defecto, `ORGANIZADOR`, `ADMIN`, `ACCESO`).

**Tarjetas de prueba (pasarela simulada):** terminada en `0002` → rechazada por fondos insuficientes;
terminada en `0069` → tarjeta vencida; cualquier otra → aprobada. `MERCADO_PAGO` siempre se aprueba.

## Tests

```bash
mvn test            # desde la raíz, corre los tests de los 5 proyectos
cd ms-reservas && ./mvnw test   # o de un proyecto
```

- Unitarios de la lógica de negocio: compra, devoluciones, búsqueda, reservas, pagos.
- **Prueba de no doble venta:** 100 clientes simultáneos por el mismo asiento / último lugar de una zona
  (`ms-reservas/.../concurrencia/`). Corre con H2 y, si hay Docker, también contra PostgreSQL con dos instancias
  del servicio compitiendo.
- Contra el sistema levantado: `./scripts/prueba-doble-venta.sh <eventoId> <sectorId> <asientoId> [clientes]`.

Detalle en [docs/05-concurrencia-y-pruebas.md](docs/05-concurrencia-y-pruebas.md).

## Configuración

| Propiedad | Servicio | Default | Descripción |
|---|---|---|---|
| `reservas.minutos-vigencia` | ms-reservas | 10 | Duración de la reserva temporal |
| `reservas.intervalo-vencimiento-ms` | ms-reservas | 30000 | Cada cuánto se liberan reservas vencidas |
| `devoluciones.horas-minimas` | ms-compras | 72 | Hasta cuántas horas antes se puede pedir la devolución |
| `pasarela.demora-ms` | ms-pagos | 300 | Latencia simulada de la pasarela |
| `servicios.*.url` | todos | `http://localhost:808X` | URL de los otros servicios |
| `servicios.timeout-ms` | eventos, reservas, compras | 2000-3000 | Timeout de las llamadas entre servicios |

## Alcance de la entrega 1 y pendientes para la entrega 2

Incluido: funcionalidades 1, 2, 3, 4, 5, 7 y 9; API REST síncrona; API Gateway; una base relacional por servicio;
mecanismo contra la doble venta probado; pasarela mock; OpenAPI; colección de requests; datos de prueba.

Pendiente (entrega 2): fila virtual, control de acceso con QR, preferencias y recomendaciones (MongoDB),
notificaciones asincrónicas, reportes, JWT con roles, resiliencia (reintentos/circuit breaker), cache, log
centralizado, contenedores para cada servicio con un único docker-compose y prueba de carga.
