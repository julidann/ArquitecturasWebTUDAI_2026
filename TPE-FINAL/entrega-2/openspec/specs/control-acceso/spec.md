# control-acceso Specification

## Purpose
Validación del QR de las entradas en el ingreso al evento (funcionalidad 8). Microservicio: `ms-compras`.

## Requirements

### Requirement: Validación del QR
El personal de acceso DEBE poder validar el QR de una entrada. Una entrada VALIDA del evento DEBE permitir el
ingreso una sola vez y pasar a UTILIZADA, registrando fecha y puerta.

#### Scenario: Entrada válida
- **WHEN** el personal escanea el QR de una entrada VALIDA del evento
- **THEN** el sistema responde PERMITIDO con sector, fila y asiento, y la entrada pasa a UTILIZADA

#### Scenario: Entrada ya utilizada
- **WHEN** se escanea un QR que ya se usó
- **THEN** el sistema responde 409 YA_UTILIZADA indicando por qué puerta ingresó

#### Scenario: QR inválido
- **WHEN** el QR no existe, es de otro evento o la entrada está DEVUELTA o CANCELADA
- **THEN** el ingreso se rechaza con el motivo (INEXISTENTE, OTRO_EVENTO, DEVUELTA, CANCELADA)

#### Scenario: Mismo QR en varias puertas a la vez
- **WHEN** 20 lectores validan el mismo QR al mismo tiempo
- **THEN** exactamente uno recibe PERMITIDO

### Requirement: Registro de ingresos
Cada validación, aceptada o rechazada, DEBE quedar registrada. Solo el rol ACCESO PUEDE validar.

#### Scenario: Resumen del evento
- **WHEN** se consulta el resumen de accesos de un evento
- **THEN** se ve cuántas personas ingresaron y cuántos intentos se rechazaron

#### Scenario: Rol incorrecto
- **WHEN** un cliente u organizador intenta validar un QR
- **THEN** el gateway responde 403
