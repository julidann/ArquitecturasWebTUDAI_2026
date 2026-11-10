# Diseño: devoluciones

## Decisiones
- **Claves de idempotencia derivadas del id de la solicitud** (`devolucion-{id}`) para liberar el lugar y para
  el reintegro. Si al aprobar falla una de las dos llamadas no se cambia nada local y el admin puede volver a
  aprobar: la operación que ya se había hecho no se repite.
- **Orden: primero liberar, después reintegrar.** Si se libera y falla el reintegro, el lugar vuelve a la venta
  y el reintegro queda pendiente de reintento (al cliente nunca se le cobra de más).
- **Reintegro por entrada** (no por compra completa), así una compra de 4 entradas puede devolver solo una.
- **Plazo de 72 hs configurable** (`devoluciones.horas-minimas`), con `Clock` inyectado para poder testearlo.
- **Notificaciones**: en la entrega 1 se registran en el log con el prefijo `[NOTIFICACION pendiente E2]`; en
  la entrega 2 se envían por un servicio de notificaciones asincrónico.
