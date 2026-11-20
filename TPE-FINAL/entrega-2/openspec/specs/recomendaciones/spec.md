# recomendaciones Specification

## Purpose
Preferencias de los clientes y recomendaciones de eventos (funcionalidad 10). Microservicio: `ms-recomendaciones` (MongoDB).

## Requirements

### Requirement: Preferencias del cliente
Un cliente DEBE poder guardar y consultar sus tipos de evento, artistas favoritos y ciudad. Los textos se
comparan sin distinguir mayúsculas, tildes ni espacios de más.

#### Scenario: Guardar
- **WHEN** el cliente guarda sus preferencias
- **THEN** se reemplazan completas y las puede consultar

#### Scenario: Sin preferencias
- **WHEN** un cliente que nunca las cargó las consulta
- **THEN** recibe listas vacías (no 404)

### Requirement: Recomendaciones personalizadas
El sistema DEBE sugerir eventos publicados y futuros con un puntaje y los motivos, según los artistas
favoritos, el historial de compras propio y de otros clientes ("también compraron") y el tipo de evento y la
ciudad preferidos. NO DEBE sugerir eventos ya comprados, cancelados ni pasados.

#### Scenario: Artista favorito
- **WHEN** un cliente tiene a "Los Piojos" como favorito y hay un evento publicado de Los Piojos
- **THEN** el evento aparece primero con el motivo correspondiente

#### Scenario: También compraron
- **WHEN** se consulta un evento
- **THEN** se listan los otros eventos que compraron sus compradores, ordenados por compradores en común

### Requirement: Datos por mensajería
El catálogo y el historial DEBEN armarse con los mensajes de eventos publicados, cancelados, reprogramados y
compras confirmadas, de forma idempotente, para que las recomendaciones no dependan de que ms-eventos esté
disponible.

#### Scenario: Mensaje repetido
- **WHEN** llega dos veces el mensaje de la misma compra
- **THEN** el historial la cuenta una sola vez
