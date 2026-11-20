# Diseño de la API REST

- Todos los endpoints públicos se acceden a través del gateway: `http://localhost:8080/api/...`
- Formato JSON, fechas ISO-8601 (`2026-12-15T21:00:00`), montos con 2 decimales.
- **(E2) Autenticación con JWT**: se obtiene con `POST /api/auth/login` y se manda en
  `Authorization: Bearer <token>`. El gateway valida el token y el rol de cada endpoint (matriz más abajo) y le
  pasa la identidad a los servicios en `X-Usuario-Id` / `X-Rol` (si el cliente manda esos headers, se pisan).
- Documentación OpenAPI completa (descripciones, ejemplos, códigos de error y esquema de seguridad `bearerAuth`):
  Swagger UI en <http://localhost:8080/swagger-ui.html> (unifica los 9 servicios) y los archivos exportados en
  [openapi/](openapi/).

## Códigos de respuesta

| Código | Cuándo |
|---|---|
| 200 | OK / reintento idempotente que devuelve el resultado original |
| 201 | Recurso creado (lugar, evento, reserva, compra aprobada, solicitud, usuario) |
| 204 | Operación sin cuerpo (salir de la fila) |
| 400 | Datos inválidos, header faltante, parámetro con formato incorrecto |
| 401 | **(E2)** Sin token, token vencido, mal firmado o de otro emisor; credenciales incorrectas en el login |
| 402 | Pago rechazado por la pasarela |
| 403 | El rol no puede usar el endpoint (gateway) o el usuario no es el dueño (servicio); turno de fila inválido |
| 404 | Recurso inexistente (o endpoint interno pedido por el gateway) |
| 409 | Conflicto con el estado actual: asiento no disponible, reserva vencida, QR ya utilizado, email ya registrado, etc. |
| 422 | Regla de negocio: límite de entradas por cliente, plazo de devolución vencido, Idempotency-Key reutilizada |
| 502 / 503 | Otro microservicio no respondió (la operación se puede reintentar); 503 también con el circuito abierto |
| 504 | El gateway no recibió respuesta del servicio a tiempo |

Formato de error común a todos los servicios:

```json
{
  "timestamp": "2026-11-07T21:30:59.262",
  "status": 409,
  "error": "Conflict",
  "mensaje": "Alguno de los asientos elegidos ya no está disponible",
  "path": "/reservas",
  "detalles": null
}
```

En errores de validación (400) `detalles` trae un mensaje por campo, por ejemplo `["fechaHora: debe ser una fecha futura"]`.

## Matriz de seguridad (E2)

Roles: **C** = CLIENTE, **O** = ORGANIZADOR, **A** = ADMIN, **P** = personal de ACCESO. "Público" = sin token.
La regla de rol la aplica el gateway (`ReglasDeAcceso`); la de "dueño" la valida cada servicio.
`SeguridadEndpointsTest` (api-gateway) prueba **cada fila con cada rol y sin token** (256 casos).

| Endpoints | Público | C | O | A | P | Regla de dueño |
|---|:-:|:-:|:-:|:-:|:-:|---|
| `POST /auth/registro`, `POST /auth/login` | ✔ | ✔ | ✔ | ✔ | ✔ | El registro siempre crea un CLIENTE |
| `GET /eventos/**`, `GET /lugares/**`, `GET /disponibilidad/**` | ✔ | ✔ | ✔ | ✔ | ✔ | |
| `GET /recomendaciones/eventos/{id}/tambien-compraron` | ✔ | ✔ | ✔ | ✔ | ✔ | |
| `GET /usuarios/me`, `GET /notificaciones` | | ✔ | ✔ | ✔ | ✔ | Cada uno ve lo suyo |
| `GET /usuarios`, `POST /usuarios`, `POST /usuarios/{id}/deshabilitar` | | | | ✔ | | |
| `POST /lugares` | | | ✔ | ✔ | | |
| `POST /eventos`, `PUT /eventos/{id}`, `PUT /eventos/{id}/precios`, `POST /eventos/{id}/publicar\|reprogramar` | | | ✔ | ✔ | | El organizador solo los suyos |
| `POST /eventos/{id}/suspender\|reanudar\|cancelar` | | | | ✔ | | |
| `POST/GET /reservas`, `GET/DELETE /reservas/{id}` | | ✔ | | | | Solo las propias |
| `POST /compras`, `GET /entradas/**`, `POST /entradas/{id}/devolucion` | | ✔ | | | | Solo las propias |
| `GET /compras`, `GET /compras/{id}`, `GET /devoluciones` | | ✔ | | ✔ | | El cliente ve las suyas, el admin todas |
| `POST /devoluciones/{id}/aprobar\|rechazar` | | | | ✔ | | |
| `POST /fila/eventos/{id}/ingresar`, `GET .../mi-turno`, `DELETE /fila/eventos/{id}` | | ✔ | | | | |
| `GET /fila/eventos/{id}/estado`, `PUT /fila/eventos/{id}/configuracion` | | | ✔ | ✔ | | El organizador solo sus eventos |
| `GET/PUT /preferencias`, `GET /recomendaciones` | | ✔ | | | | Las propias |
| `POST /accesos/validar` | | | | | ✔ | |
| `GET /accesos/eventos/{id}` | | | ✔ | ✔ | ✔ | |
| `GET /reportes/eventos/{id}` | | | ✔ | ✔ | | El organizador solo sus eventos |
| `GET /bitacora/**` | | | | ✔ | | |
| `/internal/**`, `POST /reservas/{id}/confirmar\|liberar`, `POST /disponibilidad/eventos/{id}/liberaciones` | 404 | 404 | 404 | 404 | 404 | Solo entre servicios |

## Listado de endpoints

Todas las rutas son por el gateway (prefijo `/api`). La columna **Roles** resume la matriz de arriba.

### ms-usuarios (E2)

| Método | Ruta | Parámetros | Roles | Respuestas |
|---|---|---|---|---|
| POST | `/api/auth/registro` | body `{email, nombre, password}` | público | 201, 400, 409 (email ya registrado) |
| POST | `/api/auth/login` | body `{email, password}` | público | 200 (token), 401 |
| GET | `/api/usuarios/me` | | todos | 200 |
| GET | `/api/usuarios` | | A | 200 |
| POST | `/api/usuarios` | body `{email, nombre, password, rol}` | A | 201, 400, 409 |
| POST | `/api/usuarios/{id}/deshabilitar` | | A | 200, 404 |

### ms-eventos

| Método | Ruta | Parámetros | Roles | Respuestas |
|---|---|---|---|---|
| POST | `/api/lugares` | body `LugarRequest` | O, A | 201, 400, 403 |
| GET | `/api/lugares` | `ciudad` (opcional) | público | 200 |
| GET | `/api/lugares/{id}` | | público | 200, 404 |
| GET | `/api/lugares/{id}/sectores/{sectorId}/asientos` | | público | 200, 404 |
| GET | `/api/eventos` | `q`, `tipo`, `organizadorId`, `participante`, `ciudad`, `desde`, `hasta` (yyyy-MM-dd), `orden` (fecha\|precio\|relevancia), `direccion` (asc\|desc), `pagina`, `tamanio` | público | 200, 400 |
| POST | `/api/eventos` | body `EventoRequest` (incluye `altaDemanda`) | O, A | 201, 400, 403, 404 |
| GET | `/api/eventos/{id}` | | público | 200, 404 |
| PUT | `/api/eventos/{id}` | body `EventoRequest` | O dueño, A | 200, 403, 409 |
| PUT | `/api/eventos/{id}/precios` | body `{precios:[{sectorId, precio}]}` | O dueño, A | 200, 403, 409 |
| POST | `/api/eventos/{id}/publicar` | | O dueño, A | 200, 403, 409, 502 |
| POST | `/api/eventos/{id}/reprogramar` **(E2)** | body `{fechaHora, motivo}` | O dueño, A | 200, 400, 403, 409 |
| POST | `/api/eventos/{id}/suspender` | body `{motivo}` opcional | A | 200, 403, 409 |
| POST | `/api/eventos/{id}/reanudar` | | A | 200, 403, 409 |
| POST | `/api/eventos/{id}/cancelar` | body `{motivo}` opcional | A | 200, 403, 409, 502 |

### ms-reservas

| Método | Ruta | Parámetros | Roles | Respuestas |
|---|---|---|---|---|
| GET | `/api/disponibilidad/eventos/{eventoId}` | | público | 200, 404 |
| GET | `/api/disponibilidad/eventos/{eventoId}/sectores/{sectorId}/asientos` | | público | 200, 404, 409 (no numerado) |
| POST | `/api/reservas` | body `{eventoId, sectorId, asientoIds?, cantidad?}`; **(E2)** header `X-Turno-Fila` en eventos de alta demanda | C | 201, 400, 403 (sin turno), 404, 409, 422 |
| GET | `/api/reservas` | `estado` opcional | C | 200 |
| GET | `/api/reservas/{id}` | | C (dueño) | 200, 403, 404 |
| DELETE | `/api/reservas/{id}` | | C (dueño) | 200, 403, 409 |

### ms-compras

| Método | Ruta | Parámetros | Roles | Respuestas |
|---|---|---|---|---|
| POST | `/api/compras` | body `{reservaId, medioPago}`; **`Idempotency-Key`** | C | 201, 200 (reintento), 400, 402, 403, 404, 409, 422, 503 |
| GET | `/api/compras` | | C, A | 200 |
| GET | `/api/compras/{id}` | | C (dueño), A | 200, 403, 404 |
| GET | `/api/entradas` | | C | 200 |
| GET | `/api/entradas/{id}` | | C (dueño) | 200, 403, 404 |
| GET | `/api/entradas/{id}/qr` | | C (dueño) | 200 (image/png), 403, 404 |
| POST | `/api/entradas/{id}/devolucion` | body `{motivo}` | C (dueño) | 201, 403, 404, 409, 422 |
| GET | `/api/devoluciones` | `estado` opcional | C (las suyas), A (todas) | 200 |
| POST | `/api/devoluciones/{id}/aprobar` | | A | 200, 403, 404, 409, 503 |
| POST | `/api/devoluciones/{id}/rechazar` | body `{observacion}` | A | 200, 403, 404, 409 |
| POST | `/api/accesos/validar` **(E2)** | body `{codigoQr, eventoId, puerta}` | P | 200 PERMITIDO, 409 YA_UTILIZADA / OTRO_EVENTO / DEVUELTA / CANCELADA, 404 INEXISTENTE |
| GET | `/api/accesos/eventos/{eventoId}` **(E2)** | | P, O, A | 200 (ingresaron, rechazos) |
| GET | `/api/reportes/eventos/{eventoId}` **(E2)** | | O dueño, A | 200, 403, 404 |

### ms-fila (E2)

| Método | Ruta | Parámetros | Roles | Respuestas |
|---|---|---|---|---|
| POST | `/api/fila/eventos/{id}/ingresar` | | C | 200 (posición), 404 (el evento no tiene fila), 409 (fila cerrada) |
| GET | `/api/fila/eventos/{id}/mi-turno` | | C | 200 (EN_ESPERA con posición / HABILITADO con `turno`), 404 |
| DELETE | `/api/fila/eventos/{id}` | | C | 204 |
| GET | `/api/fila/eventos/{id}/estado` | | O dueño, A | 200 (en espera, habilitados, configuración) |
| PUT | `/api/fila/eventos/{id}/configuracion` | body `{tamanioTanda, intervaloSegundos, minutosParaComprar, activa}` | O dueño, A | 200, 400, 403 |

### ms-recomendaciones (E2)

| Método | Ruta | Parámetros | Roles | Respuestas |
|---|---|---|---|---|
| GET | `/api/preferencias` | | C | 200 |
| PUT | `/api/preferencias` | body `{tiposEvento, artistasFavoritos, ciudad}` | C | 200, 400 |
| GET | `/api/recomendaciones` | `limite` (1-50, default 10) | C | 200 (eventos con `puntaje` y `motivos`) |
| GET | `/api/recomendaciones/eventos/{id}/tambien-compraron` | `limite` (1-20, default 5) | público | 200 |

### ms-notificaciones (E2)

| Método | Ruta | Parámetros | Roles | Respuestas |
|---|---|---|---|---|
| GET | `/api/notificaciones` | `limite` (1-100, default 20) | todos | 200 |

### ms-bitacora (E2)

| Método | Ruta | Parámetros | Roles | Respuestas |
|---|---|---|---|---|
| GET | `/api/bitacora` | `correlationId`, `operacion` (completa o prefijo), `servicio`, `usuarioId`, `desde`, `hasta`, `limite` | A | 200 |
| GET | `/api/bitacora/trazas/{correlationId}` | | A | 200 (pasos en orden) |

### Endpoints internos (entre servicios, no expuestos por el gateway)

| Servicio | Método y ruta | Lo llama | Para qué |
|---|---|---|---|
| ms-reservas | `POST /disponibilidad/eventos/{id}` | ms-eventos | Crear el inventario al publicar (idempotente: 201/200) |
| ms-reservas | `PATCH /disponibilidad/eventos/{id}/estado` | ms-eventos | Habilitar/deshabilitar la venta |
| ms-reservas | `PATCH /disponibilidad/eventos/{id}/fecha` **(E2)** | ms-eventos | Reprogramación |
| ms-reservas | `POST /reservas/{id}/confirmar` | ms-compras | Pago aprobado → asientos VENDIDOS |
| ms-reservas | `POST /reservas/{id}/liberar` | ms-compras | Pago rechazado → liberar lugares |
| ms-reservas | `POST /disponibilidad/eventos/{id}/liberaciones` | ms-compras | Devolución aprobada (Idempotency-Key) |
| ms-eventos | `POST /internal/eventos/{id}/agotado` y `/disponible` | ms-reservas | Actualizar el estado del evento |
| ms-compras | `POST /internal/eventos/{id}/cancelacion` | ms-eventos | Reintegrar todas las entradas |
| ms-compras | `GET /internal/eventos/{id}/compradores` **(E2)** | ms-notificaciones | A quién avisar de una cancelación o cambio |
| ms-pagos | `POST /pagos`, `GET /pagos/{id}`, `GET /pagos?compraId=`, `POST /pagos/{id}/reintegros` | ms-compras | Cobros y reintegros (Idempotency-Key); `?compraId=` lo usa el job de recuperación |
| ms-usuarios | `GET /internal/usuarios/{id}` **(E2)** | ms-notificaciones | Email del destinatario |
| ms-recomendaciones | `GET /internal/preferencias/interesados?artistas=&ciudad=` **(E2)** | ms-notificaciones | Clientes con ese artista favorito en esa ciudad |

## Ejemplos

Los ejemplos son respuestas reales del sistema levantado con los datos de `scripts/datos-prueba.sh`.
**(E2)** En vez de los headers de la entrega 1 se manda el JWT (`Authorization: Bearer ...`); los ids que se ven
en las respuestas pueden variar.

### Login (E2)

```http
POST /api/auth/login
Content-Type: application/json

{ "email": "cliente@ticketya.com", "password": "cliente123" }
```

`200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0Iiwicm9sIjoiQ0xJRU5URSIs...",
  "tipo": "Bearer",
  "expiraEn": "2026-11-19T04:48:12Z",
  "usuario": { "id": 4, "email": "cliente@ticketya.com", "nombre": "Cliente de Prueba", "rol": "CLIENTE", "activo": true }
}
```

Con la contraseña mal: `401` *"Email o contraseña incorrectos"*. Sin token en un endpoint protegido: `401`;
con un token de otro rol: `403` *"Tu rol no tiene permiso para esta operación"*.

### Crear un lugar

```http
POST /api/lugares
Authorization: Bearer <token de admin@ticketya.com>
Content-Type: application/json

{
  "nombre": "Teatro del Fuerte",
  "direccion": "Gral. Rodríguez 551",
  "ciudad": "Tandil",
  "latitud": -37.3264,
  "longitud": -59.1370,
  "sectores": [
    { "nombre": "Platea", "tipo": "NUMERADO", "filas": 8, "asientosPorFila": 12 },
    { "nombre": "Campo", "tipo": "NO_NUMERADO", "capacidad": 500 }
  ]
}
```

`201 Created`

```json
{
  "id": 2,
  "nombre": "Teatro del Fuerte",
  "direccion": "Gral. Rodríguez 551",
  "ciudad": "Tandil",
  "latitud": -37.3264,
  "longitud": -59.137,
  "sectores": [
    { "id": 4, "nombre": "Platea", "tipo": "NUMERADO", "capacidad": 96 },
    { "id": 5, "nombre": "Campo", "tipo": "NO_NUMERADO", "capacidad": 500 }
  ]
}
```

### Crear evento, definir precios y publicar

```http
POST /api/eventos
Authorization: Bearer <token de organizador2@ticketya.com>

{
  "nombre": "Hamlet",
  "descripcion": "Clásico de Shakespeare por la Comedia de Tandil",
  "tipo": "TEATRO",
  "participantes": ["Comedia de Tandil"],
  "fechaHora": "2026-11-17T21:00:00",
  "lugarId": 2,
  "altaDemanda": false,
  "limiteEntradasPorCliente": 6
}
```
→ `201` con el evento en `"estado": "BORRADOR"`.

```http
PUT /api/eventos/3/precios
Authorization: Bearer <token de organizador2@ticketya.com>

{ "precios": [ { "sectorId": 4, "precio": 18000 }, { "sectorId": 5, "precio": 12000 } ] }
```

```http
POST /api/eventos/3/publicar
Authorization: Bearer <token de organizador2@ticketya.com>
```
→ `200` con `"estado": "PUBLICADO"`. Si faltan precios: `409` *"El evento no tiene precio definido para todos los sectores"*.

### Buscar eventos

```http
GET /api/eventos?ciudad=Tandil&orden=precio&tamanio=1
```

`200 OK`

```json
{
  "contenido": [
    {
      "id": 4,
      "nombre": "Charla: Microservicios en la práctica",
      "descripcion": "Arquitectura de software para sistemas de alta demanda",
      "tipo": "CONFERENCIA",
      "organizadorId": 11,
      "participantes": [
        "Juan Pérez",
        "Ana Gómez"
      ],
      "fechaHora": "2026-11-27T21:00:00",
      "lugar": {
        "id": 2,
        "nombre": "Teatro del Fuerte",
        "ciudad": "Tandil"
      },
      "estado": "PUBLICADO",
      "altaDemanda": false,
      "limiteEntradasPorCliente": 6,
      "precioMinimo": 3000.00,
      "precios": [
        {
          "sectorId": 4,
          "sector": "Platea",
          "tipo": "NUMERADO",
          "precio": 5000.00
        },
        {
          "sectorId": 5,
          "sector": "Pullman",
          "tipo": "NUMERADO",
          "precio": 3000.00
        }
      ]
    }
  ],
  "pagina": 0,
  "tamanio": 1,
  "totalElementos": 2,
  "totalPaginas": 2
}
```

### Disponibilidad de un evento

```http
GET /api/disponibilidad/eventos/1
```

```json
{
  "eventoId": 1,
  "nombreEvento": "Los Piojos - Ritual 2026",
  "fechaEvento": "2026-12-22T21:00:00",
  "habilitado": true,
  "sectores": [
    {
      "sectorId": 1,
      "nombre": "Campo",
      "tipo": "NO_NUMERADO",
      "precio": 60000.00,
      "capacidad": 200,
      "disponibles": 200
    },
    {
      "sectorId": 2,
      "nombre": "Platea Norte",
      "tipo": "NUMERADO",
      "precio": 95000.00,
      "capacidad": 50,
      "disponibles": 49
    },
    {
      "sectorId": 3,
      "nombre": "Platea Sur",
      "tipo": "NUMERADO",
      "precio": 90000.00,
      "capacidad": 50,
      "disponibles": 50
    }
  ]
}
```

```http
GET /api/disponibilidad/eventos/1/sectores/2/asientos
```

```json
[
  {
    "asientoId": 1,
    "fila": "A",
    "numero": 1,
    "estado": "DISPONIBLE"
  },
  {
    "asientoId": 2,
    "fila": "A",
    "numero": 2,
    "estado": "VENDIDO"
  },
  {
    "asientoId": 3,
    "fila": "A",
    "numero": 3,
    "estado": "DISPONIBLE"
  }
]
```

### Reservar asientos

```http
POST /api/reservas
Authorization: Bearer <token de cliente@ticketya.com>
Content-Type: application/json

{ "eventoId": 2, "sectorId": 3, "asientoIds": [71, 72] }
```

`201 Created`

```json
{
  "id": 94,
  "usuarioId": 150,
  "eventoId": 2,
  "nombreEvento": "Festival Rock en el Bosque",
  "fechaEvento": "2027-01-06T21:00:00",
  "sectorId": 3,
  "sector": "Platea Sur",
  "tipoSector": "NUMERADO",
  "cantidad": 2,
  "precioUnitario": 70000.00,
  "montoTotal": 140000.00,
  "estado": "PENDIENTE",
  "creadaEn": "2026-11-07T21:34:30.929734133",
  "expiraEn": "2026-11-07T21:44:30.929734133",
  "segundosRestantes": 599,
  "items": [
    {
      "asientoId": 71,
      "fila": "C",
      "numero": 1
    },
    {
      "asientoId": 72,
      "fila": "C",
      "numero": 2
    }
  ]
}
```

Para un sector no numerado: `{ "eventoId": 1, "sectorId": 1, "cantidad": 2 }`.

Errores típicos: `409` *"Alguno de los asientos elegidos ya no está disponible"*,
`422` *"Se permite un máximo de 4 entradas por cliente para este evento (ya tenés 2)"*.

### Comprar (pagar la reserva)

```http
POST /api/compras
Authorization: Bearer <token de cliente@ticketya.com>
Idempotency-Key: 3f6c1b8e-2a41-4d0c-9b7e-5e2f0a1c9d33
Content-Type: application/json

{
  "reservaId": 94,
  "medioPago": {
    "tipo": "TARJETA_CREDITO",
    "numeroTarjeta": "4509953566233704",
    "titular": "Juan Perez",
    "cuotas": 1
  }
}
```

`201 Created`

```json
{
  "id": 10,
  "usuarioId": 150,
  "reservaId": 94,
  "eventoId": 2,
  "nombreEvento": "Festival Rock en el Bosque",
  "cantidad": 2,
  "montoTotal": 140000.00,
  "medioPago": "TARJETA_CREDITO",
  "ultimosDigitos": "3704",
  "pagoId": 10,
  "estado": "APROBADA",
  "detalle": null,
  "creadaEn": "2026-11-07T21:34:30.992807",
  "entradas": [
    {
      "id": 10,
      "compraId": 10,
      "eventoId": 2,
      "nombreEvento": "Festival Rock en el Bosque",
      "fechaEvento": "2027-01-06T21:00:00",
      "sectorId": 3,
      "sector": "Platea Sur",
      "asientoId": 71,
      "fila": "C",
      "numero": 1,
      "precio": 70000.00,
      "codigoQr": "fbf53842-4cd0-4b3d-ae56-3f5f0151a50b",
      "estado": "VALIDA",
      "emitidaEn": "2026-11-07T21:34:31.347939"
    },
    {
      "id": 11,
      "compraId": 10,
      "eventoId": 2,
      "nombreEvento": "Festival Rock en el Bosque",
      "fechaEvento": "2027-01-06T21:00:00",
      "sectorId": 3,
      "sector": "Platea Sur",
      "asientoId": 72,
      "fila": "C",
      "numero": 2,
      "precio": 70000.00,
      "codigoQr": "8cf2eb29-4e9f-4b38-86d8-f4261ed0d5f7",
      "estado": "VALIDA",
      "emitidaEn": "2026-11-07T21:34:31.348028"
    }
  ]
}
```

Pago rechazado (tarjeta terminada en `0002`) → `402 Payment Required`:

```json
{
  "id": 2,
  "reservaId": 2,
  "montoTotal": 120000.00,
  "medioPago": "TARJETA_DEBITO",
  "ultimosDigitos": "0002",
  "estado": "RECHAZADA",
  "detalle": "Pago rechazado: Fondos insuficientes",
  "entradas": []
}
```

Reenviar el mismo request con la misma `Idempotency-Key` → `200` con la misma compra (no se cobra de nuevo).

### Solicitar y aprobar una devolución

```http
POST /api/entradas/1/devolucion
Authorization: Bearer <token de cliente@ticketya.com>

{ "motivo": "No puedo asistir" }
```

`201 Created`

```json
{
  "id": 1,
  "entradaId": 1,
  "eventoId": 1,
  "usuarioId": 100,
  "montoAReintegrar": 95000.00,
  "motivo": "No puedo asistir",
  "estado": "PENDIENTE",
  "solicitadaEn": "2026-11-07T21:31:14.915",
  "resueltaEn": null,
  "resueltaPor": null,
  "observacion": null,
  "reintegroId": null
}
```

```http
POST /api/devoluciones/1/aprobar
Authorization: Bearer <token de admin@ticketya.com>
```
→ `200` con `"estado": "APROBADA"` y `"reintegroId": 1`. El asiento vuelve a `DISPONIBLE` y la entrada queda `DEVUELTA`.
Con menos de 72 hs al evento la solicitud devuelve `422`.

### Cancelar un evento

```http
POST /api/eventos/5/cancelar
Authorization: Bearer <token de admin@ticketya.com>

{ "motivo": "Problemas de salud del artista" }
```
→ `200` con `"estado": "CANCELADO"`. Todas las entradas pasan a `CANCELADA` y cada pago queda con su reintegro.
