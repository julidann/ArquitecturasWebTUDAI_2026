# Features y User Stories

Requerimientos de TicketYa transformados en features e historias de usuario **de todo el sistema**.
Todas las historias tienen criterios de aceptación completos (formato *Dado / Cuando / Entonces*). Las de la
entrega 2 (US-19 a US-28) se completaron en esta entrega; al lado de cada criterio importante se indica el test
que lo verifica.

Roles: **Cliente**, **Organizador**, **Administrador**, **Personal de acceso**.
Los números entre paréntesis refieren a la sección FUNCIONALIDAD de la consigna.

| Feature | Historias | Entrega | Microservicio |
|---|---|---|---|
| F1. Gestión de lugares (2) | US-01, US-02 | 1 | ms-eventos |
| F2. Gestión y publicación de eventos (1, 3) | US-03 a US-07 | 1 | ms-eventos |
| F3. Búsqueda de eventos (9) | US-08, US-09 | 1 | ms-eventos |
| F4. Reserva y compra de entradas (4, 5) | US-10 a US-15 | 1 | ms-reservas, ms-compras, ms-pagos |
| F5. Devoluciones y cancelaciones (7) | US-16 a US-18 | 1 | ms-compras |
| F6. Fila virtual (6) | US-19, US-20 | 2 | ms-fila (Redis), ms-reservas |
| F7. Control de acceso con QR (8) | US-21 | 2 | ms-compras |
| F8. Preferencias y recomendaciones (10) | US-22, US-23 | 2 | ms-recomendaciones (MongoDB) |
| F9. Notificaciones (11) | US-24 a US-26 | 2 | ms-notificaciones (MongoDB) |
| F10. Reportes para organizadores (12) | US-27 | 2 | ms-compras |
| F11. Seguridad y usuarios | US-28 | 2 | ms-usuarios + api-gateway |
| F12. Trazabilidad (consigna no funcional) | US-29 | 2 | todos + ms-bitacora (MongoDB) |

---

## F1. Gestión de lugares

### US-01 Registrar un lugar con sus sectores
**Como** organizador **quiero** registrar un lugar con sus sectores numerados y no numerados **para** poder
crear eventos en él.

Criterios de aceptación:
1. **Dado** un organizador, **cuando** registra un lugar con nombre, dirección, ciudad, latitud/longitud y un
   sector numerado de 10 filas × 20 asientos, **entonces** el lugar se crea con un sector de capacidad 200 y
   200 asientos identificados (A-1 ... J-20).
2. **Dado** un sector no numerado "Campo" con capacidad 5000, **cuando** se registra, **entonces** queda con
   capacidad 5000 y sin asientos.
3. **Dado** un sector numerado sin filas o sin asientos por fila, **cuando** se envía, **entonces** el sistema
   responde 400 indicando qué falta.
4. **Dado** un usuario con rol cliente, **cuando** intenta registrar un lugar, **entonces** recibe 403.

### US-02 Consultar lugares
**Como** organizador **quiero** ver los lugares disponibles y sus sectores **para** elegir dónde hacer mi evento.

1. **Cuando** consulto los lugares de una ciudad, **entonces** veo solo los de esa ciudad (sin importar mayúsculas).
2. **Cuando** consulto un lugar, **entonces** veo sus sectores con tipo y capacidad.
3. **Cuando** consulto los asientos de un sector numerado, **entonces** veo la grilla de filas y números.
4. **Cuando** consulto un lugar inexistente, **entonces** recibo 404.

## F2. Gestión y publicación de eventos

### US-03 Crear un evento en borrador
**Como** organizador **quiero** crear un evento con sus datos **para** prepararlo antes de publicarlo.

1. **Dado** un lugar existente, **cuando** creo un evento con nombre, descripción, tipo, participantes, fecha
   futura y lugar, **entonces** queda en estado BORRADOR y yo figuro como organizador.
2. **Cuando** no indico el límite de entradas por cliente, **entonces** se usa 6.
3. **Cuando** la fecha es pasada, **entonces** recibo 400.
4. **Mientras** está en borrador, **puedo** modificar sus datos (pero no el lugar).

### US-04 Definir precios por sector
**Como** organizador **quiero** definir el precio de cada sector **para** vender las entradas.

1. **Dado** un evento en borrador, **cuando** defino precios para los sectores, **entonces** se guardan y el
   evento muestra su precio mínimo.
2. **Cuando** defino precio para un sector de otro lugar, **entonces** recibo 409.
3. **Cuando** otro organizador intenta definir precios en mi evento, **entonces** recibe 403.
4. **Cuando** el evento ya está publicado, **entonces** no se pueden cambiar los precios (409).

### US-05 Publicar un evento
**Como** organizador **quiero** publicar mi evento **para** que los clientes lo encuentren y compren.

1. **Dado** un evento en borrador con precio en todos los sectores, **cuando** lo publico, **entonces** pasa a
   PUBLICADO, aparece en las búsquedas y sus lugares quedan disponibles para reservar.
2. **Dado** un evento al que le falta el precio de algún sector, **cuando** lo publico, **entonces** recibo 409
   y sigue en borrador.
3. **Dado** que el servicio de reservas no responde, **cuando** publico, **entonces** recibo 502 y el evento
   sigue en borrador (puedo reintentar).

### US-06 Suspender o cancelar un evento
**Como** administrador **quiero** suspender o cancelar cualquier evento **para** manejar problemas operativos.

1. **Dado** un evento publicado, **cuando** lo suspendo, **entonces** pasa a SUSPENDIDO, no se aceptan reservas
   nuevas y las pendientes se liberan. **Cuando** lo reanudo, vuelve a PUBLICADO.
2. **Dado** un evento con entradas vendidas, **cuando** lo cancelo, **entonces** pasa a CANCELADO y todas sus
   entradas pasan a reintegro (ver US-18).
3. **Cuando** un organizador o cliente intenta suspender o cancelar, **entonces** recibe 403.
4. **Cuando** intento cancelar un evento finalizado, **entonces** recibo 409.

### US-07 Evento agotado
**Como** cliente **quiero** ver cuándo un evento está agotado **para** no intentar comprar.

1. **Cuando** se vende el último lugar, **entonces** el evento pasa a AGOTADO (sigue apareciendo en las búsquedas).
2. **Cuando** se libera un lugar de un evento agotado (devolución), **entonces** vuelve a PUBLICADO.

## F3. Búsqueda de eventos

### US-08 Buscar eventos por múltiples criterios
**Como** cliente **quiero** buscar eventos por tipo, organizador, participante, ciudad y fechas **para**
encontrar lo que me interesa.

1. **Cuando** combino filtros (ej. tipo RECITAL en La Plata en diciembre), **entonces** veo solo los eventos que
   cumplen todos.
2. **Cuando** busco por participante, **entonces** no importan mayúsculas ni coincidencia exacta ("piojos"
   encuentra "Los Piojos").
3. **Cuando** el rango de fechas está invertido, **entonces** recibo 400.
4. **Siempre**: no aparecen eventos en borrador, suspendidos, cancelados ni finalizados.
5. **Los resultados** vienen paginados (máximo 50 por página).

### US-09 Ordenar los resultados
**Como** cliente **quiero** ordenar por fecha, precio o relevancia **para** decidir más rápido.

1. **Por defecto** se ordena por fecha ascendente.
2. **Cuando** ordeno por precio, **entonces** se usa el precio mínimo de cada evento (asc o desc).
3. **Cuando** busco un texto y ordeno por relevancia, **entonces** primero salen los que lo tienen en el nombre,
   luego en participantes y luego en la descripción (sin importar tildes).
4. **Cuando** el criterio de orden no existe, **entonces** recibo 400.

## F4. Reserva y compra de entradas

### US-10 Ver la disponibilidad de un evento
**Como** cliente **quiero** ver los lugares libres por sector y el mapa de asientos **para** elegir dónde sentarme.

1. **Cuando** consulto un evento publicado, **entonces** veo por sector el precio, la capacidad y los disponibles.
2. **Cuando** consulto un sector numerado, **entonces** veo cada asiento con su estado (DISPONIBLE, RESERVADO, VENDIDO).
3. **Los datos** son siempre actuales (no se cachean).

### US-11 Reservar lugares mientras pago
**Como** cliente **quiero** que los lugares que elegí queden reservados unos minutos **para** poder pagar
tranquilo sin que me los saquen.

1. **Dado** un sector numerado, **cuando** elijo asientos disponibles, **entonces** quedan RESERVADOS a mi nombre
   por 10 minutos y veo el monto total y el vencimiento.
2. **Dado** un sector no numerado, **cuando** pido N lugares y hay cupo, **entonces** se descuentan N del cupo.
3. **Cuando** alguno de los asientos ya no está libre, **entonces** recibo 409 y no se reserva ninguno.
4. **Cuando** el evento no tiene la venta habilitada, **entonces** recibo 409.
5. **Puedo** cancelar mi reserva antes de pagar y los lugares se liberan.

### US-12 Límite de entradas por cliente
**Como** organizador **quiero** limitar las entradas por cliente **para** evitar la reventa.

1. **Dado** un evento con límite 4 y un cliente con 3 entradas (reservadas o compradas), **cuando** intenta
   reservar 2 más, **entonces** recibe 422.
2. **Las entradas devueltas** no cuentan para el límite.

### US-13 Pagar y recibir mis entradas
**Como** cliente **quiero** pagar mi reserva y recibir entradas con QR **para** poder ingresar al evento.

1. **Dado** una reserva vigente, **cuando** pago y el pago se aprueba, **entonces** recibo una entrada por lugar,
   cada una con un QR único, y la compra queda registrada con el detalle y el medio de pago (solo los últimos
   4 dígitos de la tarjeta).
2. **Cuando** el pago es rechazado, **entonces** recibo 402, la reserva se libera y no se emiten entradas.
3. **Cuando** la reserva venció o es de otro cliente, **entonces** no se me cobra (409 / 403).
4. **Puedo** ver mis compras, mis entradas y la imagen del QR de cada una.

### US-14 Reintentar el pago sin que me cobren dos veces
**Como** cliente con mala conexión **quiero** poder reintentar el pago **sin** riesgo de pagar dos veces.

1. **Dado** que mandé un pago con una clave de idempotencia, **cuando** lo reenvío con la misma clave,
   **entonces** recibo el resultado original y no se vuelve a cobrar.
2. **Dado** que la pasarela no respondió, **cuando** reintento con la misma clave, **entonces** la compra se
   completa con un único cobro.
3. **Dado** que el pago se aprobó pero mi reserva venció en el medio, **entonces** se me reintegra el dinero
   automáticamente.

### US-15 Nunca vender dos veces la misma entrada
**Como** TicketYa **quiero** garantizar que un asiento o lugar se venda una sola vez **para** no tener dos
personas con la misma entrada.

1. **Dado** 100 clientes que intentan reservar el mismo asiento al mismo tiempo, **entonces** solo uno lo logra.
2. **Dado** 100 clientes por el último lugar de una zona, **entonces** solo uno lo logra y la zona nunca queda
   con cupo negativo.
3. **Dado** dos instancias del servicio de reservas, **entonces** se mantiene la garantía.
4. **Dado** que el pago se confirma justo cuando vence la reserva, **entonces** gana solo una de las dos
   operaciones.

## F5. Devoluciones y cancelaciones

### US-16 Solicitar la devolución de una entrada
**Como** cliente **quiero** pedir la devolución de una entrada **para** recuperar mi dinero si no puedo ir.

1. **Dado** una entrada válida mía y más de 72 horas antes del evento, **cuando** pido la devolución,
   **entonces** queda una solicitud PENDIENTE.
2. **Cuando** faltan menos de 72 horas, **entonces** recibo 422.
3. **Cuando** la entrada no es mía o ya no es válida, **entonces** recibo 403 / 409.
4. **No puedo** tener dos solicitudes pendientes para la misma entrada.

### US-17 Resolver devoluciones
**Como** administrador **quiero** aprobar o rechazar devoluciones **para** controlar los reintegros.

1. **Cuando** apruebo, **entonces** el lugar se libera para la venta, se pide el reintegro a la pasarela y la
   entrada queda DEVUELTA.
2. **Cuando** rechazo, **entonces** la entrada sigue válida y queda registrada mi observación.
3. **Si** falla la pasarela al aprobar, **entonces** la solicitud sigue pendiente y puedo reintentar sin
   reintegros duplicados.
4. **Solo** un administrador puede resolver (403 para otros roles).

### US-18 Reintegro automático por cancelación
**Como** cliente **quiero** que me devuelvan el dinero si cancelan el evento **sin** tener que pedirlo.

1. **Cuando** se cancela un evento, **entonces** todas las entradas válidas pasan a CANCELADA y se pide el
   reintegro de cada una.
2. **Se notifica** a cada comprador por email (entrega 2, ver US-25).
3. **Las solicitudes** de devolución pendientes de ese evento se cierran.
4. **Si** se procesa dos veces la cancelación, no hay reintegros duplicados.

---

## F6. Fila virtual (entrega 2)

> Decisión: la fila se habilita solo para los eventos marcados como **alta demanda** (`altaDemanda: true`).
> Para el resto se reserva directo como en la entrega 1.

### US-19 Ingresar a la fila virtual
**Como** cliente **quiero** entrar a una fila virtual al abrirse la venta de un evento de alta demanda **para**
comprar en orden de llegada y sin que se caiga el sitio.

Criterios de aceptación:
1. **Dado** un evento de alta demanda recién publicado, **cuando** ms-eventos emite `evento.publicado`,
   **entonces** ms-fila abre la fila automáticamente (sin intervención del organizador).
2. **Dado** un cliente logueado, **cuando** ingresa a la fila, **entonces** recibe su posición (1 = el próximo),
   la cantidad de clientes esperando y una espera estimada en segundos.
3. **Cuando** ingresa dos veces, **entonces** conserva su lugar original (no vuelve al final ni ocupa dos lugares).
4. **Cuando** consulta "mi turno", **entonces** ve su posición actualizada; si ya fue habilitado ve el estado
   HABILITADO y un **turno firmado** con su vencimiento.
5. **Dado** un evento de alta demanda, **cuando** un cliente intenta reservar **sin** turno, con un turno vencido,
   de otro evento o de otro cliente, **entonces** recibe 403 (FilaVirtualIT).
6. **Cuando** abandona la fila (DELETE), **entonces** sale y los que estaban detrás avanzan un lugar.
7. **Si** el evento se cancela, **entonces** la fila se cierra y no entra nadie más.

### US-20 Habilitar tandas
**Como** sistema **quiero** habilitar a los clientes de a tandas **para** no saturar el servicio de reservas.

1. **Cada** N segundos (por defecto 10) se habilita la siguiente tanda de M clientes (por defecto 100) en orden
   estricto de llegada.
2. **El organizador** del evento (o un ADMIN) puede cambiar el tamaño de tanda, el intervalo y los minutos para
   comprar; otro organizador recibe 403.
3. **Un cliente habilitado** tiene un tiempo máximo (por defecto 10 minutos) para empezar su reserva; pasado ese
   tiempo el turno deja de servir.
4. **Aunque** ms-fila corra en varias instancias, cada tanda se habilita una sola vez (lock en Redis) y ningún
   cliente se habilita dos veces (FilaServiceTest).

## F7. Control de acceso (entrega 2)

### US-21 Validar el QR en el ingreso
**Como** personal de acceso **quiero** escanear el QR de la entrada **para** dejar pasar solo entradas válidas.

1. **Dado** un QR de una entrada VALIDA del evento, **cuando** lo escaneo, **entonces** el sistema responde
   PERMITIDO con el sector / fila / asiento y la entrada pasa a UTILIZADA con la fecha y la puerta.
2. **Cuando** escaneo un QR ya utilizado, **entonces** responde 409 YA_UTILIZADA indicando por qué puerta y a
   qué hora ingresó.
3. **Cuando** el QR es de otro evento, no existe, o la entrada está DEVUELTA o CANCELADA, **entonces** se rechaza
   con el motivo (OTRO_EVENTO, INEXISTENTE, DEVUELTA, CANCELADA).
4. **Si** el mismo QR se escanea a la vez en varias puertas, **entonces** entra una sola vez (AccesoServiceTest,
   20 lectores concurrentes).
5. **Cada** validación (aceptada o no) queda registrada; el organizador puede ver cuántos ingresaron.
6. **Solo** el rol ACCESO puede validar (403 para los demás).

## F8. Preferencias y recomendaciones (entrega 2)

### US-22 Registrar mis preferencias
**Como** cliente **quiero** guardar los tipos de evento, mis artistas favoritos y mi ciudad **para** recibir
sugerencias y avisos.

1. **Cuando** guardo mis preferencias (PUT), **entonces** se reemplazan completas y las puedo consultar (GET).
2. **Si** nunca las cargué, **entonces** el GET devuelve listas vacías (no 404).
3. **Los artistas** y la ciudad se comparan sin importar mayúsculas ni espacios de más ("los piojos" = "Los Piojos").
4. **Solo** el rol CLIENTE tiene preferencias.

### US-23 Recibir recomendaciones
**Como** cliente **quiero** ver eventos sugeridos según mis preferencias y mi historial **para** descubrir eventos.

1. **Cuando** pido mis recomendaciones, **entonces** veo eventos con un puntaje y los motivos ("participa X, uno
   de tus artistas favoritos", "quienes compraron ... también compraron este", "te gustan los eventos de tipo
   RECITAL y es en tu ciudad").
2. **No** se sugieren eventos que ya compré, cancelados ni pasados.
3. **Cualquiera** (sin login) puede ver, para un evento, "quienes compraron entradas para este evento también
   compraron para..." ordenado por cantidad de compradores en común.
4. **Las** recomendaciones se arman con datos que llegan por mensajes (eventos publicados, cancelados,
   reprogramados y compras confirmadas): si ms-eventos está caído las recomendaciones siguen funcionando.
5. **Si** un mensaje de compra llega repetido, el historial no se duplica.

## F9. Notificaciones (entrega 2)

### US-24 Aviso de compra confirmada
**Como** cliente **quiero** recibir un email cuando se confirma mi compra **para** tener el comprobante.

1. **Cuando** una compra queda APROBADA, **entonces** recibo un email con el evento, la fecha, la cantidad de
   entradas y el total (FlujoCompraIT).
2. **El envío** es asincrónico: si ms-notificaciones o el proveedor de email están caídos, la compra igual se
   aprueba y el email sale cuando se recuperan (outbox + RabbitMQ + reintentos).
3. **Si** el mensaje llega dos veces, recibo un solo email.

### US-25 Aviso de cancelación o cambio de fecha
**Como** cliente **quiero** enterarme si cancelan o cambian la fecha de un evento para el que tengo entradas.

1. **Cuando** un ADMIN cancela un evento, **entonces** cada comprador recibe un email con el motivo y el aviso de
   que se le reintegra el dinero (CancelacionYCambioIT).
2. **Cuando** el organizador reprograma el evento, **entonces** cada comprador recibe un email con la fecha
   anterior y la nueva; sus entradas siguen siendo válidas.
3. **Si** se reprograma dos veces, se avisa las dos veces.

### US-26 Aviso de artista favorito en mi ciudad
**Como** cliente **quiero** que me avisen cuando un artista favorito publica un evento en mi ciudad.

1. **Dado** que tengo a "Los Piojos" como favorito y vivo en La Plata, **cuando** se publica un evento de Los
   Piojos en La Plata, **entonces** recibo un email (FlujoCompraIT).
2. **Si** el evento es en otra ciudad, no recibo nada.

Criterios comunes a US-24..26:
- Cada usuario ve su bandeja con `GET /api/notificaciones` (estado ENVIADA, PENDIENTE, FALLIDA o DESCARTADA).
- **Si** el proveedor falla, la notificación queda PENDIENTE y se reintenta con espera creciente hasta 5 veces;
  después queda FALLIDA. A un usuario deshabilitado no se le envía (DESCARTADA).

## F10. Reportes (entrega 2)

### US-27 Reportes del organizador
**Como** organizador **quiero** ver las entradas vendidas y devueltas por sector, el porcentaje de ocupación y la
recaudación de mis eventos **para** seguir las ventas.

1. **Cuando** pido el reporte de un evento mío, **entonces** veo por sector: capacidad, vendidas, devueltas,
   canceladas, vigentes, ingresaron, % de ocupación y recaudación (neta de reintegros), y los totales.
2. **El** % de ocupación = entradas vigentes / capacidad del sector.
3. **Cuando** pido el reporte de un evento de otro organizador, **entonces** recibo 403; un ADMIN puede ver todos.
4. **Si** ms-reservas no responde, el reporte sale igual con las ventas pero sin la ocupación
   (`ocupacionDisponible: false`).

## F11. Seguridad (entrega 2)

### US-28 Autenticación y roles
**Como** usuario **quiero** iniciar sesión y que cada endpoint solo sea accesible por los roles correspondientes.

1. **Cuando** me registro (POST /api/auth/registro), **entonces** se crea siempre un usuario CLIENTE (los otros
   roles los da de alta un ADMIN).
2. **Cuando** inicio sesión con email y contraseña correctos, **entonces** recibo un JWT con mi id y rol que
   vence a las 8 horas; si me equivoco recibo 401 con el mismo mensaje sea el email o la contraseña.
3. **Sin** token solo puedo usar los endpoints públicos (catálogo, búsqueda, disponibilidad, login/registro).
4. **Con** un token de otro rol recibo 403; con un token vencido, con otra firma o de otro emisor, 401.
5. **No** puedo hacerme pasar por otro usuario mandando los headers `X-Usuario-Id` / `X-Rol` (el gateway los pisa).
6. **Cada** endpoint queda accesible solo para sus roles (matriz en docs/04; SeguridadEndpointsTest prueba
   cada endpoint con cada rol).

## F12. Trazabilidad (entrega 2)

### US-29 Seguir una operación en el log centralizado
**Como** administrador **quiero** consultar las operaciones críticas y seguir una operación entre servicios
**para** investigar problemas.

1. **Cada** request HTTP queda en el log con endpoint, usuario, rol, código de respuesta y tiempo.
2. **Cada** operación crítica (compra, cobro, devolución, cancelación, ingreso al evento, ...) queda registrada en
   ms-bitacora con el `X-Correlation-Id` que generó el gateway.
3. **Cuando** consulto `/api/bitacora/trazas/{correlationId}`, **entonces** veo en orden lo que registró cada
   servicio (ej. una compra: ms-pagos → ms-reservas → ms-compras) (FlujoCompraIT).
4. **Si** RabbitMQ no está disponible, la operación de negocio no falla (el registro queda en el log local).
