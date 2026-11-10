## ADDED Requirements

### Requirement: Datos de un evento
Cada evento DEBE tener ID, nombre, descripción, tipo (RECITAL, FESTIVAL, TEATRO, DEPORTE, CONFERENCIA,
OTRO), organizador, participantes, fecha y hora, lugar y estado (BORRADOR, PUBLICADO, AGOTADO,
SUSPENDIDO, CANCELADO, FINALIZADO). Además DEBE tener un límite de entradas por cliente (6 por defecto)
y una marca de alta demanda (se usa en la entrega 2 para la fila virtual).

#### Scenario: Crear evento
- **WHEN** un organizador crea un evento con fecha futura en un lugar existente
- **THEN** el evento queda en estado BORRADOR y su organizador es el usuario que lo creó

#### Scenario: Fecha pasada
- **WHEN** se intenta crear un evento con fecha pasada
- **THEN** el sistema responde 400

### Requirement: Precios por sector
El organizador DEBE poder definir el precio de cada sector del lugar mientras el evento esté en BORRADOR.
Solo el organizador dueño del evento (o un administrador) puede modificarlo.

#### Scenario: Definir precios
- **WHEN** el organizador define precio 50000 para la platea y 30000 para el campo
- **THEN** el evento guarda ambos precios y su precio mínimo es 30000

#### Scenario: Sector de otro lugar
- **WHEN** se define precio para un sector que no pertenece al lugar del evento
- **THEN** el sistema responde 409

#### Scenario: Otro organizador
- **WHEN** un organizador intenta modificar un evento de otro organizador
- **THEN** el sistema responde 403

### Requirement: Publicación
El sistema DEBE permitir publicar un evento en BORRADOR solo si todos los sectores del lugar tienen precio.
Al publicar, DEBE crearse en `ms-reservas` el inventario de lugares del evento (un registro por asiento
numerado y un cupo por sector no numerado). Si eso falla, el evento DEBE seguir en BORRADOR.

#### Scenario: Publicar con todos los precios
- **WHEN** el organizador publica un evento con precio en todos los sectores
- **THEN** el evento pasa a PUBLICADO, aparece en las búsquedas y se puede reservar

#### Scenario: Publicar sin precios
- **WHEN** el organizador publica un evento al que le falta el precio de algún sector
- **THEN** el sistema responde 409 y el evento sigue en BORRADOR

#### Scenario: Falla ms-reservas al publicar
- **WHEN** ms-reservas no responde durante la publicación
- **THEN** el sistema responde 502 y el evento sigue en BORRADOR

### Requirement: Suspensión y cancelación por administradores
Un administrador DEBE poder suspender (y reanudar) un evento publicado o agotado, y cancelar cualquier
evento no finalizado. Suspender o cancelar DEBE deshabilitar la venta y liberar las reservas pendientes.
Cancelar un evento con ventas DEBE pasar todas las entradas vendidas a reintegro y notificar a los compradores.

#### Scenario: Cancelar evento con entradas vendidas
- **WHEN** un administrador cancela un evento publicado
- **THEN** el evento pasa a CANCELADO, se deshabilita la venta en ms-reservas
- **AND** ms-compras pasa las entradas válidas a CANCELADA y pide el reintegro de cada una a ms-pagos

#### Scenario: Organizador intenta cancelar
- **WHEN** un organizador intenta cancelar un evento
- **THEN** el sistema responde 403

#### Scenario: Reintento de cancelación
- **WHEN** la cancelación falló a mitad de camino y el administrador la vuelve a pedir
- **THEN** se completa sin reintegrar dos veces las entradas ya reintegradas

#### Scenario: Evento suspendido
- **WHEN** un administrador suspende un evento
- **THEN** no se pueden crear reservas nuevas hasta que se reanude

### Requirement: Evento agotado
Cuando se vende el último lugar de un evento, el evento DEBE pasar a AGOTADO. Si luego se libera un lugar
por una devolución, DEBE volver a PUBLICADO.

#### Scenario: Se vende el último lugar
- **WHEN** se confirma la compra del último lugar disponible
- **THEN** el evento pasa a AGOTADO
