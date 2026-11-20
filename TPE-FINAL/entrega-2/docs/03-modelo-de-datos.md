# Modelo de datos

Cada microservicio tiene su propia base. **(E2)** Hay tres tecnologías según el tipo de dato:

| Motor | Bases / uso | Servicios |
|---|---|---|
| PostgreSQL 16 | `usuarios_db`, `eventos_db`, `reservas_db`, `compras_db`, `pagos_db` (una base por servicio) | ms-usuarios, ms-eventos, ms-reservas, ms-compras, ms-pagos |
| MongoDB 7 | `recomendaciones_db`, `notificaciones_db`, `bitacora_db` | ms-recomendaciones, ms-notificaciones, ms-bitacora |
| Redis 7 | cache del catálogo (claves `ticketya:*`) y fila virtual (claves `fila:*`) | ms-eventos, ms-fila |

La justificación de cada elección está en [06-decisiones-tecnologicas.md](06-decisiones-tecnologicas.md).

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
        bigint organizador_id "ref ms-usuarios.usuario"
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

**(E2) Outbox:** ms-eventos tiene además la tabla `mensaje_saliente` (misma estructura que en ms-compras) donde
se guardan los mensajes `evento.publicado`, `evento.cancelado` y `evento.reprogramado` en la misma transacción
que el cambio del evento.

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
        boolean alta_demanda "copia (E2): exige turno de la fila"
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
        bigint usuario_id "ref ms-usuarios.usuario"
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
    ENTRADA ||--o{ REGISTRO_ACCESO : "se escanea (E2)"

    COMPRA {
        bigint id PK
        varchar idempotency_key UK
        bigint usuario_id "ref ms-usuarios.usuario"
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
        timestamp utilizada_en "E2: ingreso al evento"
        varchar puerta_ingreso "E2"
    }
    REGISTRO_ACCESO {
        bigint id PK
        bigint evento_id "índice"
        varchar codigo_qr "el que se escaneó"
        bigint entrada_id "null si el QR no existe"
        bigint personal_id "usuario ACCESO"
        varchar puerta
        varchar resultado "PERMITIDO | YA_UTILIZADA | OTRO_EVENTO | INEXISTENTE | DEVUELTA | CANCELADA"
        timestamp fecha
    }
    MENSAJE_SALIENTE {
        bigint id PK
        varchar routing_key "compra.confirmada"
        varchar payload "JSON"
        varchar correlation_id
        timestamp creado_en
        timestamp publicado_en "null = pendiente (índice)"
        int intentos
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

## ms-usuarios (`usuarios_db`) (E2)

```mermaid
erDiagram
    USUARIO {
        bigint id PK
        varchar email UK
        varchar nombre
        varchar password_hash "BCrypt"
        varchar rol "CLIENTE | ORGANIZADOR | ADMIN | ACCESO"
        boolean activo
        timestamp creado_en
    }
```

Usuarios precargados para probar: ver README (admin, 2 organizadores, 2 clientes y personal de acceso).

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
| `usuario_id`, `organizador_id`, `personal_id` | todas | ms-usuarios.usuario | Sale del `sub` del JWT (el gateway lo pasa en `X-Usuario-Id`). |
| `eventoId`, `usuarioId`, `compraId` | MongoDB (recomendaciones, notificaciones, bitácora) | ms-eventos / ms-usuarios / ms-compras | Llegan en los mensajes de RabbitMQ. |

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
| Fecha del evento (E2) | ms-eventos `evento` | ms-reservas, ms-compras, ms-recomendaciones | fecha y hora | Al reprogramar (REST a reservas, mensaje al resto). |
| Evento publicado (E2) | ms-eventos `evento` | ms-recomendaciones `eventos` | nombre, tipo, participantes, ciudad, fecha, precio mínimo, cancelado | Mensajes `evento.*`. |
| Compra confirmada (E2) | ms-compras `compra` | ms-recomendaciones `historial_compras` | compra, usuario, evento, cantidad | Mensaje `compra.confirmada`. |

Riesgos y cómo se manejan:
- **(E2)** La fecha de un evento publicado se puede cambiar con `POST /eventos/{id}/reprogramar`: ms-eventos
  actualiza la copia de ms-reservas en el momento (REST) y emite `evento.reprogramado`, con el que ms-compras
  actualiza `compra.fecha_evento` (plazo de 72 hs para devolver) y ms-recomendaciones su catálogo
  (consistencia eventual). El resto de los datos se sigue pudiendo modificar solo en borrador.
- El estado AGOTADO en `ms-eventos` es informativo y se actualiza *best effort*: la disponibilidad real siempre se
  consulta en `ms-reservas`.

## Datos NoSQL (E2)

### MongoDB

En MongoDB no hay un esquema fijo; estos son los documentos que guarda cada servicio y sus índices.

```mermaid
erDiagram
    PREFERENCIAS {
        long _id "usuarioId"
        array tiposEvento "[RECITAL, TEATRO]"
        array artistasFavoritos "[Los Piojos]"
        string ciudad
        array artistasNormalizados "índice"
        string ciudadNormalizada "índice"
        date actualizadas
    }
    EVENTOS {
        long _id "eventoId"
        string nombre
        string tipo
        array participantes
        array participantesNormalizados "índice"
        string lugar
        string ciudad
        string ciudadNormalizada "índice"
        date fechaHora
        decimal precioMinimo
        bool cancelado
    }
    HISTORIAL_COMPRAS {
        long _id "compraId (idempotencia)"
        long usuarioId "índice"
        long eventoId "índice"
        int cantidad
        date fecha
    }
    PREFERENCIAS ||--o{ HISTORIAL_COMPRAS : "usuarioId"
    EVENTOS ||--o{ HISTORIAL_COMPRAS : "eventoId"
```

*`recomendaciones_db` (ms-recomendaciones).* Índice compuesto `{usuarioId, eventoId}` en el historial. La
recomendación "quienes compraron también compraron" es un *aggregation pipeline*:
`$match` (compradores del evento) → `$group` por `eventoId` con `$addToSet: usuarioId` → `$size` → `$sort`.

```mermaid
erDiagram
    NOTIFICACIONES {
        string _id "ObjectId"
        string clave UK "TIPO:referencia:usuarioId (idempotencia)"
        long usuarioId "índice"
        string tipo "COMPRA_CONFIRMADA | EVENTO_CANCELADO | EVENTO_REPROGRAMADO | ARTISTA_FAVORITO"
        string canal "EMAIL"
        long eventoId
        string asunto
        string mensaje
        string destino "email"
        string estado "PENDIENTE | ENVIADA | FALLIDA | DESCARTADA"
        int intentos
        string ultimoError
        string correlationId
        date creada
        date enviada
        date proximoIntento "índice {estado, proximoIntento}"
        long version "bloqueo optimista"
    }
    REGISTROS {
        string _id "ObjectId"
        string correlationId "índice"
        string servicio
        string operacion "índice {operacion, fecha}"
        string resultado
        long usuarioId "índice"
        object detalle "libre: cada operación manda lo suyo"
        date fecha "índice"
        date recibido
    }
```

*`notificaciones_db` (ms-notificaciones)* guarda `notificaciones`; *`bitacora_db` (ms-bitacora)* guarda `registros`.

### Redis

| Clave | Tipo | Servicio | Contenido | Vida |
|---|---|---|---|---|
| `ticketya:eventos::{id}` | string (JSON) | ms-eventos | Detalle de un evento | TTL 10 min / se borra al cambiar |
| `ticketya:busquedas::{filtros}` | string (JSON) | ms-eventos | Página de resultados | TTL 60 s |
| `ticketya:lugares::{id}`, `ticketya:lugares-ciudad::{ciudad}` | string (JSON) | ms-eventos | Lugares | TTL 1 h |
| `fila:{evento}:config` | hash | ms-fila | organizador, tamaño de tanda, intervalo, minutos para comprar, activa | mientras dure la venta |
| `fila:{evento}:seq` | contador (`INCR`) | ms-fila | Número de llegada | ídem |
| `fila:{evento}:espera` | sorted set | ms-fila | clientes en espera, score = número de llegada | ídem |
| `fila:{evento}:habilitados` | contador | ms-fila | cuántos se habilitaron (para la posición estimada) | ídem |
| `fila:{evento}:hab:{usuario}` | string | ms-fila | cliente habilitado | TTL = minutos para comprar |
| `fila:{evento}:tanda` | string (`SET NX PX`) | ms-fila | lock: solo una instancia habilita cada tanda | TTL = intervalo |
