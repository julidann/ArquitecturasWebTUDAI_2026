# Tareas

## 1. ms-reservas
- [x] 1.1 Inventario: `EventoDisponibilidad`, `SectorDisponibilidad`, `AsientoDisponibilidad` + inicialización idempotente
- [x] 1.2 `POST /reservas` con UPDATE condicional (numerado y no numerado) y límite por cliente
- [x] 1.3 Confirmar / liberar / cancelar con transiciones condicionales
- [x] 1.4 Job de vencimiento cada 30 s
- [x] 1.5 Liberación de lugares vendidos (para devoluciones) idempotente por clave
- [x] 1.6 Aviso de evento agotado a ms-eventos

## 2. ms-pagos
- [x] 2.1 Pasarela mock con tarjetas de prueba y demora configurable
- [x] 2.2 Cobro y reintegro idempotentes por `Idempotency-Key`

## 3. ms-compras
- [x] 3.1 `POST /compras` con Idempotency-Key, estados PENDIENTE/APROBADA/RECHAZADA/ERROR
- [x] 3.2 Emisión de entradas con QR (UUID) y endpoint de imagen PNG
- [x] 3.3 Compensación (reintegro) si la reserva no se puede confirmar

## 4. Pruebas
- [x] 4.1 Unitarias de reservas, pagos y compras
- [x] 4.2 Prueba de concurrencia: 100 clientes por el mismo asiento / último lugar (H2 y PostgreSQL)
- [x] 4.3 Prueba con dos instancias de ms-reservas sobre la misma base (Testcontainers)
- [x] 4.4 Script `scripts/prueba-doble-venta.sh` contra el sistema levantado
