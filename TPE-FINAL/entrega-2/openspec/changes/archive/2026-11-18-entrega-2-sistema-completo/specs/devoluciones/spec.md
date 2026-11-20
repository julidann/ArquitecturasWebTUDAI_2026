## ADDED Requirements

### Requirement: Aviso de devolución y cancelación
Las devoluciones aprobadas y las cancelaciones de evento DEBEN quedar en la bitácora y, en el caso de la
cancelación, DEBEN generar un aviso a cada comprador (ver `notificaciones`).

#### Scenario: Cancelación con compradores
- **WHEN** se cancela un evento con entradas vendidas
- **THEN** se reintegran las entradas y cada comprador recibe el aviso
