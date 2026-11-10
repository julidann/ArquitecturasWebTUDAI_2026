## ADDED Requirements

### Requirement: Búsqueda por múltiples criterios
El sistema DEBE permitir buscar eventos combinando: texto libre, tipo de evento, organizador, participante,
ciudad y rango de fechas. Solo DEBEN aparecer eventos PUBLICADOS o AGOTADOS. Los resultados DEBEN estar
paginados (máximo 50 por página).

#### Scenario: Combinar filtros
- **WHEN** se busca `organizadorId=12&ciudad=tandil`
- **THEN** se devuelven solo los eventos de ese organizador en Tandil

#### Scenario: Participante sin distinguir mayúsculas
- **WHEN** se busca `participante=los piojos`
- **THEN** se devuelven los eventos donde participa "Los Piojos"

#### Scenario: Rango de fechas
- **WHEN** se busca con `desde` y `hasta`
- **THEN** se devuelven los eventos con fecha dentro del rango, ambos días incluidos

#### Scenario: Rango invertido
- **WHEN** `desde` es posterior a `hasta`
- **THEN** el sistema responde 400

#### Scenario: Borradores y cancelados no se muestran
- **WHEN** se busca sin filtros
- **THEN** no aparecen eventos en BORRADOR, SUSPENDIDO, CANCELADO ni FINALIZADO

### Requirement: Ordenamiento
El sistema DEBE permitir ordenar por fecha (por defecto), por precio (precio mínimo del evento) o por
relevancia, en forma ascendente o descendente.

#### Scenario: Orden por precio descendente
- **WHEN** se busca con `orden=precio&direccion=desc`
- **THEN** los eventos vienen del más caro al más barato según su precio mínimo

#### Scenario: Orden por relevancia
- **WHEN** se busca `q=piojos&orden=relevancia`
- **THEN** primero aparecen los eventos con "piojos" en el nombre, luego los que lo tienen en participantes
  y por último en la descripción; sin distinguir tildes ni mayúsculas

#### Scenario: Orden inválido
- **WHEN** se envía `orden=nombre`
- **THEN** el sistema responde 400
