# Tareas

## 1. Lugares
- [x] 1.1 Entidades `Lugar`, `Sector`, `Asiento` y generación de asientos por filas
- [x] 1.2 `POST /lugares`, `GET /lugares`, `GET /lugares/{id}`, `GET /lugares/{id}/sectores/{sectorId}/asientos`
- [x] 1.3 Validaciones (sector numerado sin filas, rol)

## 2. Eventos
- [x] 2.1 Entidades `Evento`, `PrecioSector`, enums de tipo y estado
- [x] 2.2 Crear / modificar evento en borrador, definir precios
- [x] 2.3 Publicar (con inicialización de inventario en ms-reservas)
- [x] 2.4 Suspender, reanudar y cancelar (solo ADMIN)
- [x] 2.5 Endpoints internos `agotado` / `disponible` para ms-reservas

## 3. Pruebas y documentación
- [x] 3.1 Tests unitarios de `EventoService` (publicación, permisos, cancelación)
- [x] 3.2 Anotaciones OpenAPI con ejemplos y códigos de error
- [x] 3.3 Requests en la colección Postman
