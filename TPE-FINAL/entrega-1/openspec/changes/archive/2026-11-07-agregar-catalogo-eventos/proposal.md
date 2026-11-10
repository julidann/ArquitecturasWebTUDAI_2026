# Agregar catálogo de lugares y eventos

## Why
Es la base de todo el sistema: sin lugares, sectores y eventos publicados no hay nada para buscar ni comprar.
Cubre las funcionalidades 1, 2 y 3 de la consigna y la parte de cancelación de la 7.

## What Changes
- Nuevo microservicio `ms-eventos` con su propia base (`eventos_db`).
- ABM de lugares con sectores numerados (se generan los asientos) y no numerados (capacidad).
- Eventos con ciclo de vida BORRADOR → PUBLICADO → AGOTADO / SUSPENDIDO / CANCELADO / FINALIZADO.
- Precios por sector definidos por el organizador; publicación solo con todos los precios cargados.
- Al publicar se crea el inventario en `ms-reservas`; al cancelar se avisa a `ms-reservas` y `ms-compras`.
- Suspensión y cancelación solo para administradores.

## Impact
- Specs afectadas: `gestion-lugares` (nueva), `gestion-eventos` (nueva).
- Código: `ms-eventos`, endpoints internos en `ms-reservas` y `ms-compras`, rutas en `api-gateway`.
