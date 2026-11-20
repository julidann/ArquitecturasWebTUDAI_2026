# seguridad Specification

## Purpose
Autenticación con JWT y autorización por rol. Microservicios: `ms-usuarios` y `api-gateway`.

## Requirements

### Requirement: Registro e inicio de sesión
El registro público DEBE crear siempre un usuario CLIENTE. El inicio de sesión DEBE devolver un JWT firmado
con el id y el rol del usuario y un vencimiento (8 horas por defecto).

#### Scenario: Login correcto
- **WHEN** un usuario inicia sesión con email y contraseña correctos
- **THEN** recibe un JWT con su id y su rol

#### Scenario: Credenciales inválidas
- **WHEN** el email no existe o la contraseña es incorrecta
- **THEN** recibe 401 con el mismo mensaje en los dos casos

### Requirement: Autorización por rol en el gateway
El gateway DEBE validar firma, emisor y vencimiento del JWT y permitir cada endpoint solo a los roles
definidos en la matriz de permisos. DEBE reemplazar los headers `X-Usuario-Id` y `X-Rol` que mande el cliente
por los del token. Los endpoints internos NO DEBEN estar expuestos.

#### Scenario: Sin token
- **WHEN** se llama a un endpoint protegido sin token
- **THEN** el gateway responde 401

#### Scenario: Rol incorrecto
- **WHEN** un usuario llama a un endpoint que no corresponde a su rol
- **THEN** el gateway responde 403

#### Scenario: Token inválido
- **WHEN** el token está vencido, firmado con otra clave o es de otro emisor
- **THEN** el gateway responde 401

#### Scenario: Headers falsificados
- **WHEN** un cliente manda `X-Rol: ADMIN`
- **THEN** el servicio recibe el rol del token y no el del header
