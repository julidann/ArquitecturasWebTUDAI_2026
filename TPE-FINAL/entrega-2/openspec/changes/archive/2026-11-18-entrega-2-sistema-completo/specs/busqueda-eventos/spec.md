## ADDED Requirements

### Requirement: Cache
El detalle de un evento y los resultados de búsqueda DEBEN cachearse en Redis. El detalle DEBE invalidarse al
modificar el evento y las búsquedas DEBEN tener un TTL corto. La disponibilidad NO se cachea.

#### Scenario: Detalle modificado
- **WHEN** se modifica un evento cacheado
- **THEN** la siguiente consulta devuelve los datos nuevos
