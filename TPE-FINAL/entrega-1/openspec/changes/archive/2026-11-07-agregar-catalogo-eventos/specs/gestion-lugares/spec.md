## ADDED Requirements

### Requirement: Alta de lugar con sectores
El sistema DEBE permitir a organizadores y administradores registrar un lugar con ID, nombre, dirección,
ciudad, ubicación (latitud/longitud) y al menos un sector. Un sector DEBE ser NUMERADO (filas y asientos
identificados) o NO_NUMERADO (zona con capacidad máxima).

#### Scenario: Lugar con sector numerado
- **WHEN** un organizador crea un lugar con un sector NUMERADO de 10 filas y 20 asientos por fila
- **THEN** se crea el lugar con un sector de capacidad 200
- **AND** se generan 200 asientos identificados por fila (A, B, C, ...) y número (1..20)

#### Scenario: Lugar con sector no numerado
- **WHEN** un organizador crea un lugar con un sector NO_NUMERADO "Campo" de capacidad 5000
- **THEN** se crea el sector con capacidad 5000 y sin asientos

#### Scenario: Sector numerado sin filas
- **WHEN** se envía un sector NUMERADO sin `filas` o sin `asientosPorFila`
- **THEN** el sistema responde 400 indicando el dato faltante

#### Scenario: Cliente intenta crear un lugar
- **WHEN** un usuario con rol CLIENTE intenta crear un lugar
- **THEN** el sistema responde 403

### Requirement: Consulta de lugares
El sistema DEBE permitir listar lugares (opcionalmente filtrando por ciudad), ver el detalle de un lugar
con sus sectores y la grilla de asientos de un sector numerado.

#### Scenario: Filtrar lugares por ciudad
- **WHEN** se consulta `GET /lugares?ciudad=Tandil`
- **THEN** se devuelven solo los lugares de esa ciudad, sin importar mayúsculas

#### Scenario: Lugar inexistente
- **WHEN** se consulta un lugar que no existe
- **THEN** el sistema responde 404
