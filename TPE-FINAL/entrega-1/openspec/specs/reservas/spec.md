# reservas Specification

## Purpose
Disponibilidad de lugares por evento y reserva temporal mientras el cliente paga (funcionalidad 4), con el
mecanismo que garantiza que una entrada nunca se venda dos veces (funcionalidad 5). Microservicio: `ms-reservas`.

## Requirements

### Requirement: Consulta de disponibilidad
El sistema DEBE informar, para un evento publicado, los lugares disponibles por sector y el estado de cada
asiento de un sector numerado (DISPONIBLE, RESERVADO, VENDIDO). Esta información NO DEBE cachearse.

#### Scenario: Mapa de asientos
- **WHEN** un cliente consulta los asientos de la platea de un evento
- **THEN** recibe cada asiento con su fila, número y estado actual

### Requirement: Reserva temporal
El sistema DEBE permitir a un cliente reservar asientos específicos (sector numerado) o una cantidad de
lugares (sector no numerado). La reserva DEBE quedar PENDIENTE durante 10 minutos con el monto calculado
según el precio del sector. La reserva DEBE ser todo o nada.

#### Scenario: Reservar asientos numerados
- **WHEN** un cliente reserva los asientos C-11 y C-12 de la platea, ambos disponibles
- **THEN** la reserva queda PENDIENTE, vence en 10 minutos y los asientos pasan a RESERVADO

#### Scenario: Asiento ocupado
- **WHEN** un cliente intenta reservar un asiento RESERVADO o VENDIDO
- **THEN** el sistema responde 409 y no se reserva ninguno de los asientos del pedido

#### Scenario: Zona sin cupo suficiente
- **WHEN** un cliente pide 3 lugares en una zona que tiene 2 disponibles
- **THEN** el sistema responde 409

#### Scenario: Asientos en zona no numerada
- **WHEN** un cliente envía `asientoIds` para un sector no numerado
- **THEN** el sistema responde 400

#### Scenario: Evento sin venta habilitada
- **WHEN** un cliente intenta reservar en un evento suspendido o cancelado
- **THEN** el sistema responde 409

### Requirement: Límite de entradas por cliente
El sistema DEBE impedir que un cliente tenga, para un mismo evento, más entradas (reservas pendientes +
compradas no devueltas) que el límite del evento.

#### Scenario: Supera el límite
- **WHEN** un cliente con 3 entradas en un evento con límite 4 intenta reservar 2 más
- **THEN** el sistema responde 422

### Requirement: Vencimiento y liberación
Si la reserva no se confirma en 10 minutos DEBE pasar a VENCIDA y sus lugares DEBEN volver a estar
disponibles. Si el pago es rechazado o el cliente cancela, la reserva DEBE pasar a LIBERADA con el mismo efecto.

#### Scenario: Reserva vencida
- **WHEN** pasan 10 minutos sin que se confirme el pago
- **THEN** la reserva pasa a VENCIDA, los asientos vuelven a DISPONIBLE y la reserva ya no se puede confirmar

#### Scenario: Cliente cancela
- **WHEN** el cliente cancela su reserva pendiente
- **THEN** los lugares se liberan

### Requirement: No doble venta
Un mismo asiento (o el último lugar de una zona) NUNCA DEBE quedar reservado o vendido para dos clientes,
aun con muchos pedidos simultáneos y con varias instancias de ms-reservas ejecutándose a la vez. El control
DEBE hacerse en la base de datos compartida y no en memoria.

#### Scenario: Muchos clientes por el mismo asiento
- **WHEN** 100 clientes intentan reservar el mismo asiento al mismo tiempo
- **THEN** exactamente uno obtiene la reserva y los otros 99 reciben 409

#### Scenario: Último lugar de una zona
- **WHEN** 100 clientes intentan reservar el último lugar de una zona al mismo tiempo
- **THEN** exactamente uno lo obtiene y la zona queda con 0 disponibles (nunca negativo)

#### Scenario: Dos instancias del servicio
- **WHEN** los pedidos simultáneos se reparten entre dos instancias de ms-reservas sobre la misma base
- **THEN** exactamente uno obtiene el asiento

#### Scenario: Confirmación y vencimiento a la vez
- **WHEN** la confirmación del pago y el vencimiento de la reserva ocurren al mismo tiempo
- **THEN** gana solo una de las dos operaciones y el estado de la reserva y del asiento quedan coherentes
