## ADDED Requirements

### Requirement: Reprogramación
El organizador del evento (o un ADMIN) DEBE poder cambiar la fecha de un evento publicado a una fecha futura.
Las entradas vendidas siguen siendo válidas y los compradores DEBEN ser avisados.

#### Scenario: Cambio de fecha
- **WHEN** el organizador reprograma su evento
- **THEN** el evento queda con la nueva fecha y se publica `evento.reprogramado` con la fecha anterior y la nueva

#### Scenario: Otro organizador
- **WHEN** un organizador intenta reprogramar un evento ajeno
- **THEN** el sistema responde 403

### Requirement: Mensajes de eventos
ms-eventos DEBE publicar `evento.publicado`, `evento.cancelado` y `evento.reprogramado` mediante un outbox
transaccional.

#### Scenario: Publicación
- **WHEN** se publica un evento
- **THEN** se emite `evento.publicado` con los datos que necesitan fila, recomendaciones y notificaciones
