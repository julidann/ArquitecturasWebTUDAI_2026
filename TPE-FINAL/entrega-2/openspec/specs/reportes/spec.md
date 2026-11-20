# reportes Specification

## Purpose
Reportes de ventas para organizadores (funcionalidad 12). Microservicio: `ms-compras`.

## Requirements

### Requirement: Reporte de un evento
El organizador DEBE poder ver, para cada sector de su evento, la capacidad, las entradas vendidas, devueltas,
canceladas, vigentes y que ya ingresaron, el porcentaje de ocupación (vigentes / capacidad) y la recaudación
neta de reintegros, con los totales del evento.

#### Scenario: Reporte propio
- **WHEN** el organizador pide el reporte de su evento
- **THEN** recibe los datos por sector y los totales

#### Scenario: Evento ajeno
- **WHEN** un organizador pide el reporte de un evento de otro organizador
- **THEN** el sistema responde 403; un ADMIN puede ver todos

#### Scenario: ms-reservas no disponible
- **WHEN** no se puede consultar la capacidad en ms-reservas
- **THEN** el reporte sale con las ventas y `ocupacionDisponible = false`
