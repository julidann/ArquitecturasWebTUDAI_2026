## ADDED Requirements

### Requirement: Correlation id
El gateway DEBE asignar un `X-Correlation-Id` a cada request (o respetar el que venga) y cada servicio DEBE
propagarlo en las llamadas REST, en los mensajes de RabbitMQ y en sus logs.

#### Scenario: Seguir una compra
- **WHEN** un ADMIN consulta la traza del correlation id de una compra
- **THEN** ve en orden los registros de ms-pagos, ms-reservas y ms-compras

### Requirement: Bitácora de operaciones críticas
Cada servicio DEBE registrar las operaciones críticas (compra, cobro, reintegro, devolución, cancelación,
reprogramación, ingreso al evento) con usuario, resultado y fecha en el log centralizado. Una falla del
broker NO DEBE hacer fallar la operación de negocio. Solo un ADMIN PUEDE consultar la bitácora.

#### Scenario: Consulta con filtros
- **WHEN** un ADMIN busca por operación, usuario, servicio o rango de fechas
- **THEN** recibe los registros que cumplen los filtros, del más nuevo al más viejo

#### Scenario: Log de requests
- **WHEN** cualquier servicio atiende un request
- **THEN** queda en su log el endpoint, usuario, rol, código de respuesta, tiempo y correlation id
