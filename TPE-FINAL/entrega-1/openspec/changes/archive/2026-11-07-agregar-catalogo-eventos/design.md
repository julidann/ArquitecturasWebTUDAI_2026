# Diseño: catálogo de lugares y eventos

## Decisiones
- **Asientos generados en el alta del lugar.** El organizador indica filas y asientos por fila y el sistema
  genera la grilla (filas A, B, ... AA, AB). Simplifica la carga y evita errores de tipeo. Lugares con
  formas irregulares quedan fuera de alcance.
- **Precio mínimo desnormalizado** en `evento.precio_minimo` para poder ordenar las búsquedas por precio
  sin joins ni subconsultas.
- **Inventario en otro servicio.** Los asientos "físicos" viven en `ms-eventos`, pero el estado de cada
  asiento para un evento vive en `ms-reservas`. Al publicar se le envía a `ms-reservas` una copia de los
  datos que necesita (sectores, precios, asientos, fecha, límite por cliente) para que el camino crítico de la
  compra no dependa de `ms-eventos`.
- **Publicación atómica desde el punto de vista del organizador.** La llamada a `ms-reservas` se hace dentro
  de la transacción local: si falla, se hace rollback y el evento queda en BORRADOR. La inicialización del
  inventario es idempotente para poder reintentar.
- **Cancelación reintentable.** Cancelar llama a `ms-reservas` (deshabilitar venta) y a `ms-compras`
  (reintegros). Ambos son idempotentes, así que si algo falla el admin vuelve a cancelar. En la entrega 2 se
  evalúa pasarlo a mensajería asincrónica (saga con compensaciones).
- **Roles por header** (`X-Rol`) en la entrega 1; en la entrega 2 se reemplaza por JWT.

## Alternativas descartadas
- Guardar el estado de los asientos en `ms-eventos`: mezcla datos de catálogo (muy leídos, cacheables) con
  datos de alta contención durante la venta.
