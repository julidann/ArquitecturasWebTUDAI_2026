## ADDED Requirements

### Requirement: Turno de fila en eventos de alta demanda
Para reservar en un evento de alta demanda, el cliente DEBE presentar en el header `X-Turno-Fila` un turno
vigente emitido por ms-fila para ese evento y ese cliente.

#### Scenario: Sin turno
- **WHEN** un cliente reserva en un evento de alta demanda sin turno, con un turno vencido o de otro cliente
- **THEN** el sistema responde 403

#### Scenario: Con turno
- **WHEN** un cliente habilitado reserva con su turno vigente
- **THEN** la reserva se crea normalmente
