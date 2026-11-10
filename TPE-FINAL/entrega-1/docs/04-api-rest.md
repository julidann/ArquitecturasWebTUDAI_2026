# Diseño de la API REST

- Todos los endpoints públicos se acceden a través del gateway: `http://localhost:8080/api/...`
- Formato JSON, fechas ISO-8601 (`2026-12-15T21:00:00`), montos con 2 decimales.
- **Entrega 1 sin autenticación**: el usuario se identifica con los headers `X-Usuario-Id` (numérico) y
  `X-Rol` (`CLIENTE` por defecto, `ORGANIZADOR`, `ADMIN`, `ACCESO`).
- Documentación OpenAPI completa (descripciones, ejemplos y códigos de error): Swagger UI en
  <http://localhost:8080/swagger-ui.html> (unifica los 4 servicios) y los archivos exportados en [openapi/](openapi/).

## Códigos de respuesta

| Código | Cuándo |
|---|---|
| 200 | OK / reintento idempotente que devuelve el resultado original |
| 201 | Recurso creado (lugar, evento, reserva, compra aprobada, solicitud) |
| 204 | Operación interna sin cuerpo |
| 400 | Datos inválidos, header faltante, parámetro con formato incorrecto |
| 402 | Pago rechazado por la pasarela |
| 403 | El rol o el usuario no puede hacer la operación |
| 404 | Recurso inexistente (o endpoint interno pedido por el gateway) |
| 409 | Conflicto con el estado actual: asiento no disponible, reserva vencida, evento no está en borrador, solicitud ya resuelta, etc. |
| 422 | Regla de negocio: límite de entradas por cliente, plazo de devolución vencido, Idempotency-Key reutilizada |
| 502 / 503 | Otro microservicio no respondió (la operación se puede reintentar) |

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

## Listado de endpoints

### ms-eventos

| Método | Ruta (gateway) | Parámetros / headers | Respuestas |
|---|---|---|---|
| POST | `/api/lugares` | body `LugarRequest`; `X-Rol` ORGANIZADOR/ADMIN | 201, 400, 403 |
| GET | `/api/lugares` | `ciudad` (opcional) | 200 |
| GET | `/api/lugares/{id}` | | 200, 404 |
| GET | `/api/lugares/{id}/sectores/{sectorId}/asientos` | | 200, 404 |
| GET | `/api/eventos` | `q`, `tipo`, `organizadorId`, `participante`, `ciudad`, `desde`, `hasta` (yyyy-MM-dd), `orden` (fecha\|precio\|relevancia), `direccion` (asc\|desc), `pagina`, `tamanio` | 200, 400 |
| POST | `/api/eventos` | body `EventoRequest`; `X-Usuario-Id`, `X-Rol` ORGANIZADOR/ADMIN | 201, 400, 403, 404 |
| GET | `/api/eventos/{id}` | | 200, 404 |
| PUT | `/api/eventos/{id}` | body `EventoRequest`; organizador dueño | 200, 403, 409 |
| PUT | `/api/eventos/{id}/precios` | body `{precios:[{sectorId, precio}]}`; organizador dueño | 200, 403, 409 |
| POST | `/api/eventos/{id}/publicar` | organizador dueño | 200, 403, 409, 502 |
| POST | `/api/eventos/{id}/suspender` | body `{motivo}` opcional; ADMIN | 200, 403, 409 |
| POST | `/api/eventos/{id}/reanudar` | ADMIN | 200, 403, 409 |
| POST | `/api/eventos/{id}/cancelar` | body `{motivo}` opcional; ADMIN | 200, 403, 409, 502 |

### ms-reservas

| Método | Ruta (gateway) | Parámetros / headers | Respuestas |
|---|---|---|---|
| GET | `/api/disponibilidad/eventos/{eventoId}` | | 200, 404 |
| GET | `/api/disponibilidad/eventos/{eventoId}/sectores/{sectorId}/asientos` | | 200, 404, 409 (no numerado) |
| POST | `/api/reservas` | body `{eventoId, sectorId, asientoIds?, cantidad?}`; `X-Usuario-Id` | 201, 400, 404, 409, 422 |
| GET | `/api/reservas` | `estado` opcional; `X-Usuario-Id` | 200 |
| GET | `/api/reservas/{id}` | `X-Usuario-Id` | 200, 403, 404 |
| DELETE | `/api/reservas/{id}` | `X-Usuario-Id` (dueño) | 200, 403, 409 |

### ms-compras

| Método | Ruta (gateway) | Parámetros / headers | Respuestas |
|---|---|---|---|
| POST | `/api/compras` | body `{reservaId, medioPago}`; `X-Usuario-Id`, **`Idempotency-Key`** | 201, 200 (reintento), 400, 402, 403, 404, 409, 422, 503 |
| GET | `/api/compras` | `X-Usuario-Id` | 200 |
| GET | `/api/compras/{id}` | `X-Usuario-Id` | 200, 403, 404 |
| GET | `/api/entradas` | `X-Usuario-Id` | 200 |
| GET | `/api/entradas/{id}` | `X-Usuario-Id` | 200, 403, 404 |
| GET | `/api/entradas/{id}/qr` | `X-Usuario-Id` | 200 (image/png), 403, 404 |
| POST | `/api/entradas/{id}/devolucion` | body `{motivo}`; `X-Usuario-Id` | 201, 403, 404, 409, 422 |
| GET | `/api/devoluciones` | `estado` opcional; `X-Usuario-Id`, `X-Rol` | 200 |
| POST | `/api/devoluciones/{id}/aprobar` | `X-Usuario-Id`, `X-Rol: ADMIN` | 200, 403, 404, 409, 503 |
| POST | `/api/devoluciones/{id}/rechazar` | body `{observacion}`; ADMIN | 200, 403, 404, 409 |

### Endpoints internos (entre servicios, no expuestos por el gateway)

| Servicio | Método y ruta | Lo llama | Para qué |
|---|---|---|---|
| ms-reservas | `POST /disponibilidad/eventos/{id}` | ms-eventos | Crear el inventario al publicar (idempotente: 201/200) |
| ms-reservas | `PATCH /disponibilidad/eventos/{id}/estado` | ms-eventos | Habilitar/deshabilitar la venta |
| ms-reservas | `POST /reservas/{id}/confirmar` | ms-compras | Pago aprobado → asientos VENDIDOS |
| ms-reservas | `POST /reservas/{id}/liberar` | ms-compras | Pago rechazado → liberar lugares |
| ms-reservas | `POST /disponibilidad/eventos/{id}/liberaciones` | ms-compras | Devolución aprobada (Idempotency-Key) |
| ms-eventos | `POST /internal/eventos/{id}/agotado` y `/disponible` | ms-reservas | Actualizar el estado del evento |
| ms-compras | `POST /internal/eventos/{id}/cancelacion` | ms-eventos | Reintegrar todas las entradas |
| ms-pagos | `POST /pagos`, `GET /pagos/{id}`, `GET /pagos?compraId=`, `POST /pagos/{id}/reintegros` | ms-compras | Cobros y reintegros (Idempotency-Key) |

## Ejemplos

Los ejemplos son respuestas reales del sistema levantado con los datos de `scripts/datos-prueba.sh`.

### Crear un lugar

```http
POST /api/lugares
X-Usuario-Id: 1
X-Rol: ADMIN
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
X-Usuario-Id: 11
X-Rol: ORGANIZADOR

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
X-Usuario-Id: 11
X-Rol: ORGANIZADOR

{ "precios": [ { "sectorId": 4, "precio": 18000 }, { "sectorId": 5, "precio": 12000 } ] }
```

```http
POST /api/eventos/3/publicar
X-Usuario-Id: 11
X-Rol: ORGANIZADOR
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
X-Usuario-Id: 150
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
X-Usuario-Id: 150
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
X-Usuario-Id: 100

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
X-Usuario-Id: 1
X-Rol: ADMIN
```
→ `200` con `"estado": "APROBADA"` y `"reintegroId": 1`. El asiento vuelve a `DISPONIBLE` y la entrada queda `DEVUELTA`.
Con menos de 72 hs al evento la solicitud devuelve `422`.

### Cancelar un evento

```http
POST /api/eventos/5/cancelar
X-Usuario-Id: 1
X-Rol: ADMIN

{ "motivo": "Problemas de salud del artista" }
```
→ `200` con `"estado": "CANCELADO"`. Todas las entradas pasan a `CANCELADA` y cada pago queda con su reintegro.
