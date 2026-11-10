# Modelo de datos

Cada microservicio tiene su propia base PostgreSQL (`eventos_db`, `reservas_db`, `compras_db`, `pagos_db`).
**No hay claves foráneas entre bases**: las referencias a datos de otro servicio se guardan como un id
"lógico" (columna `*_id` sin FK) y, cuando hace falta, con una copia de los datos necesarios.

Convenciones de los diagramas: `PK` clave primaria, `FK` clave foránea dentro de la misma base, `UK` única,
y `"ref ms-x"` indica un id que apunta a una entidad de otro microservicio.

Las tablas las genera Hibernate (`ddl-auto: update`) a partir de las entidades JPA.

## ms-eventos (`eventos_db`)

```mermaid
erDiagram
    LUGAR ||--o{ SECTOR : tiene
    SECTOR ||--o{ ASIENTO : tiene
    LUGAR ||--o{ EVENTO : "se realiza en"
    EVENTO ||--o{ PRECIO_SECTOR : define
    SECTOR ||--o{ PRECIO_SECTOR : "tiene precio en"
    EVENTO ||--o{ EVENTO_PARTICIPANTE : tiene

    LUGAR {
        bigint id PK
        varchar nombre
        varchar direccion
        varchar ciudad
        double latitud
        double longitud
    }
    SECTOR {
        bigint id PK
        bigint lugar_id FK
        varchar nombre
        varchar tipo "NUMERADO | NO_NUMERADO"
        int capacidad
    }
    ASIENTO {
        bigint id PK
        bigint sector_id FK "UK(sector_id, fila, numero)"
        varchar fila
        int numero
    }
    EVENTO {
        bigint id PK
        varchar nombre
        varchar descripcion
        varchar tipo "RECITAL | FESTIVAL | TEATRO | DEPORTE | CONFERENCIA | OTRO"
        bigint organizador_id "ref usuario (E2)"
        timestamp fecha_hora
        bigint lugar_id FK
        varchar estado "BORRADOR | PUBLICADO | AGOTADO | SUSPENDIDO | CANCELADO | FINALIZADO"
        boolean alta_demanda
        int limite_entradas_cliente
        numeric precio_minimo "desnormalizado para ordenar"
        timestamp creado_en
        timestamp actualizado_en
    }
    EVENTO_PARTICIPANTE {
        bigint evento_id FK
        varchar participante
    }
    PRECIO_SECTOR {
        bigint id PK
        bigint evento_id FK "UK(evento_id, sector_id)"
        bigint sector_id FK
        numeric precio
    }
```

## ms-reservas (`reservas_db`)

```mermaid
erDiagram
    EVENTO_DISPONIBILIDAD ||--o{ SECTOR_DISPONIBILIDAD : tiene
    EVENTO_DISPONIBILIDAD ||--o{ ASIENTO_DISPONIBILIDAD : tiene
    EVENTO_DISPONIBILIDAD ||--o{ RESERVA : recibe
    RESERVA ||--o{ RESERVA_ITEM : contiene
    RESERVA ||--o{ ASIENTO_DISPONIBILIDAD : "bloquea (reserva_id)"
    RESERVA ||--o{ LIBERACION : "devoluciones"

    EVENTO_DISPONIBILIDAD {
        bigint evento_id PK "ref ms-eventos.evento"
        varchar nombre_evento "copia"
        timestamp fecha_evento "copia"
        int limite_por_cliente "copia"
        boolean habilitado
        timestamp actualizado_en
    }
    SECTOR_DISPONIBILIDAD {
        bigint id PK
        bigint evento_id "UK(evento_id, sector_id)"
        bigint sector_id "ref ms-eventos.sector"
        varchar nombre "copia"
        varchar tipo "copia"
        numeric precio "copia de precio_sector"
        int capacidad
        int disponibles "UPDATE condicional"
        bigint version
    }
    ASIENTO_DISPONIBILIDAD {
        bigint id PK
        bigint evento_id "UK(evento_id, asiento_id)"
        bigint sector_id "ref ms-eventos.sector"
        bigint asiento_id "ref ms-eventos.asiento"
        varchar fila "copia"
        int numero "copia"
        varchar estado "DISPONIBLE | RESERVADO | VENDIDO"
        bigint reserva_id "reserva que lo tiene"
        bigint version
    }
    RESERVA {
        bigint id PK
        bigint usuario_id "ref usuario (E2)"
        bigint evento_id
        bigint sector_id
        varchar tipo_sector
        int cantidad
        int cantidad_devuelta
        numeric precio_unitario
        numeric monto_total
        varchar estado "PENDIENTE | CONFIRMADA | LIBERADA | VENCIDA"
        timestamp creada_en
        timestamp expira_en "creada_en + 10 min"
        timestamp finalizada_en
    }
    RESERVA_ITEM {
        bigint id PK
        bigint reserva_id FK
        bigint asiento_id "null si no numerado"
        varchar fila
        int numero
    }
    LIBERACION {
        bigint id PK
        varchar clave UK "Idempotency-Key"
        bigint evento_id
        bigint reserva_id
        bigint sector_id
        bigint asiento_id
        int cantidad
        timestamp creada_en
    }
```

Índices adicionales: `asiento_disponibilidad(evento_id, sector_id)`, `asiento_disponibilidad(reserva_id)`,
`reserva(estado, expira_en)` (para el job de vencimiento) y `reserva(usuario_id, evento_id)` (límite por cliente).

## ms-compras (`compras_db`)

```mermaid
erDiagram
    COMPRA ||--o{ ENTRADA : emite
    ENTRADA ||--o{ SOLICITUD_DEVOLUCION : "puede tener"

    COMPRA {
        bigint id PK
        varchar idempotency_key UK
        bigint usuario_id "ref usuario (E2)"
        bigint reserva_id UK "ref ms-reservas.reserva"
        bigint evento_id "ref ms-eventos.evento"
        varchar nombre_evento "copia"
        timestamp fecha_evento "copia (regla 72 hs)"
        bigint sector_id
        varchar sector_nombre "copia"
        int cantidad
        numeric monto_total
        varchar medio_pago "TARJETA_CREDITO | TARJETA_DEBITO | MERCADO_PAGO"
        varchar ultimos_digitos
        bigint pago_id "ref ms-pagos.pago"
        varchar estado "PENDIENTE | APROBADA | RECHAZADA | ERROR"
        varchar detalle
        timestamp creada_en
        timestamp actualizada_en
    }
    ENTRADA {
        bigint id PK
        bigint compra_id FK
        bigint usuario_id
        bigint evento_id
        bigint sector_id
        bigint asiento_id "ref ms-eventos.asiento, null si no numerado"
        varchar fila
        int numero
        numeric precio
        varchar codigo_qr UK "UUID"
        varchar estado "VALIDA | DEVUELTA | CANCELADA | UTILIZADA"
        varchar lugar_activo UK "evento-asiento mientras está VALIDA"
        timestamp emitida_en
    }
    SOLICITUD_DEVOLUCION {
        bigint id PK
        bigint entrada_id FK
        bigint usuario_id
        varchar motivo
        varchar estado "PENDIENTE | APROBADA | RECHAZADA"
        timestamp solicitada_en
        timestamp resuelta_en
        bigint resuelta_por "admin"
        varchar observacion
        bigint reintegro_id "ref ms-pagos.reintegro"
    }
```

## ms-pagos (`pagos_db`)

```mermaid
erDiagram
    PAGO ||--o{ REINTEGRO : tiene

    PAGO {
        bigint id PK
        varchar idempotency_key UK "compra-{id}"
        bigint compra_id "ref ms-compras.compra"
        bigint usuario_id
        numeric monto
        varchar medio_pago
        varchar ultimos_digitos
        varchar titular
        int cuotas
        varchar estado "APROBADO | RECHAZADO"
        varchar codigo_autorizacion
        varchar motivo_rechazo
        timestamp creado_en
    }
    REINTEGRO {
        bigint id PK
        bigint pago_id FK
        varchar idempotency_key UK "devolucion-{id} | cancelacion-entrada-{id} | compensacion-compra-{id}"
        numeric monto
        varchar motivo
        varchar estado
        timestamp creado_en
    }
```

## Relaciones entre servicios

```mermaid
flowchart LR
    subgraph eventos_db
        E[evento] --- S[sector] --- A[asiento]
    end
    subgraph reservas_db
        ED[evento_disponibilidad] --- SD[sector_disponibilidad]
        AD[asiento_disponibilidad]
        R[reserva]
    end
    subgraph compras_db
        C[compra] --- EN[entrada] --- SOL[solicitud_devolucion]
    end
    subgraph pagos_db
        P[pago] --- RI[reintegro]
    end
    E -. evento_id .-> ED
    S -. sector_id .-> SD
    A -. asiento_id .-> AD
    R -. reserva_id .-> C
    E -. evento_id .-> C
    A -. asiento_id .-> EN
    C -. compra_id .-> P
    P -. pago_id .-> C
    RI -. reintegro_id .-> SOL
```

| Referencia | Desde | Hacia | Cómo se mantiene |
|---|---|---|---|
| `evento_id` | reservas_db, compras_db | ms-eventos.evento | Se fija al publicar / comprar; el id nunca cambia. |
| `sector_id`, `asiento_id` | reservas_db, compras_db | ms-eventos.sector / asiento | Ídem. |
| `reserva_id` | compras_db.compra (UNIQUE) | ms-reservas.reserva | Una compra por reserva. |
| `compra_id` | pagos_db.pago | ms-compras.compra | Se manda al cobrar. |
| `pago_id`, `reintegro_id` | compras_db | ms-pagos | Se guardan con la respuesta de ms-pagos. |
| `usuario_id`, `organizador_id` | todas | (ms-usuarios, entrega 2) | Hoy viene del header `X-Usuario-Id`. |

## Entidades solapadas entre bases

Hay datos que aparecen en más de una base. Es una decisión consciente para que cada servicio sea autónomo
(sobre todo `ms-reservas`, que no debe depender de nadie durante una venta masiva).

| Concepto | Dueño (fuente de verdad) | Copia en | Qué se copia | Cuándo se sincroniza |
|---|---|---|---|---|
| Evento | ms-eventos `evento` | ms-reservas `evento_disponibilidad` | nombre, fecha, límite por cliente; estado como `habilitado` | Al publicar (alta) y al suspender/reanudar/cancelar (habilitado). |
| Evento | ms-eventos `evento` | ms-compras `compra` | nombre y fecha | Al comprar (snapshot). |
| Sector + precio | ms-eventos `sector`, `precio_sector` | ms-reservas `sector_disponibilidad` | nombre, tipo, precio, capacidad | Al publicar. Los precios no se pueden cambiar después de publicar, así que la copia no queda desactualizada. |
| Asiento | ms-eventos `asiento` | ms-reservas `asiento_disponibilidad` | fila y número (+ estado propio) | Al publicar. |
| Asiento vendido | ms-reservas `asiento_disponibilidad` (VENDIDO) | ms-compras `entrada` (`lugar_activo`) | evento + asiento | Al confirmar la compra / al devolver. |
| Pago | ms-pagos `pago` | ms-compras `compra` | id del pago, medio, últimos dígitos | Con la respuesta del cobro. |

Riesgos y cómo se manejan:
- Si se cambiara la fecha de un evento publicado, las copias quedarían viejas. Por eso en la entrega 1 solo se
  pueden modificar eventos en borrador; el cambio de evento publicado (con notificación) es de la entrega 2.
- El estado AGOTADO en `ms-eventos` es informativo y se actualiza *best effort*: la disponibilidad real siempre se
  consulta en `ms-reservas`.
