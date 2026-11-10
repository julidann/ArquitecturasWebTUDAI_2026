# Agregar búsqueda de eventos

## Why
El cliente tiene que poder encontrar eventos desde la app combinando criterios (funcionalidad 9).

## What Changes
- `GET /eventos` con filtros combinables: texto, tipo, organizador, participante, ciudad, rango de fechas.
- Ordenamiento por fecha, precio o relevancia, ascendente o descendente, con paginación.
- Solo se muestran eventos PUBLICADOS o AGOTADOS.

## Impact
- Specs afectadas: `busqueda-eventos` (nueva).
- Código: `ms-eventos` (`BusquedaService`, `EventoSpecifications`).
