# notificaciones Specification

## Purpose
Avisos a los clientes por email simulado (funcionalidad 11). Microservicio: `ms-notificaciones` (MongoDB).

## Requirements

### Requirement: Avisos al cliente
El sistema DEBE avisar por email al cliente cuando se confirma su compra, cuando se cancela o se reprograma
un evento para el que tiene entradas, y cuando un artista favorito publica un evento en su ciudad.

#### Scenario: Compra confirmada
- **WHEN** una compra queda APROBADA
- **THEN** el comprador recibe un email con el evento, la fecha, las entradas y el total

#### Scenario: Cancelación
- **WHEN** un ADMIN cancela un evento
- **THEN** cada comprador recibe un email con el motivo y el aviso del reintegro

#### Scenario: Reprogramación
- **WHEN** el organizador cambia la fecha del evento
- **THEN** cada comprador recibe un email con la fecha anterior y la nueva

#### Scenario: Artista favorito en la ciudad
- **WHEN** se publica un evento de un artista favorito del cliente en su ciudad
- **THEN** el cliente recibe un email; si es en otra ciudad no recibe nada

### Requirement: Entrega confiable
Las notificaciones DEBEN procesarse de forma asincrónica: una falla del proveedor de email o de
ms-notificaciones NO DEBE afectar la operación que la originó. Un aviso NO DEBE enviarse dos veces.

#### Scenario: Proveedor caído
- **WHEN** el proveedor de email no responde
- **THEN** la notificación queda PENDIENTE y se reintenta con espera creciente hasta 5 veces; después queda FALLIDA

#### Scenario: Mensaje duplicado
- **WHEN** llega dos veces el mismo mensaje
- **THEN** se genera una sola notificación

#### Scenario: Usuario deshabilitado
- **WHEN** el destinatario está deshabilitado
- **THEN** la notificación queda DESCARTADA
