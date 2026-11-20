# fila-virtual Specification

## Purpose
Fila virtual para la apertura de venta de eventos de alta demanda (funcionalidad 6). Microservicio: `ms-fila` (Redis).

## Requirements

### Requirement: Apertura automática de la fila
Para un evento marcado como de alta demanda, el sistema DEBE abrir la fila virtual automáticamente al
publicarse el evento y DEBE cerrarla si el evento se cancela.

#### Scenario: Publicación de un evento de alta demanda
- **WHEN** ms-eventos publica un evento con `altaDemanda = true`
- **THEN** ms-fila recibe el mensaje `evento.publicado` y abre la fila con la configuración por defecto

#### Scenario: Evento cancelado
- **WHEN** se cancela un evento con fila abierta
- **THEN** la fila se cierra y un nuevo ingreso responde 409

### Requirement: Ingreso por orden de llegada
Un cliente DEBE poder entrar a la fila y conocer su posición, la cantidad de clientes esperando y una espera
estimada. Volver a ingresar NO DEBE cambiar su posición.

#### Scenario: Ingreso
- **WHEN** un cliente entra a la fila de un evento
- **THEN** recibe su posición (1 = el próximo), los clientes en espera y la espera estimada en segundos

#### Scenario: Reingreso
- **WHEN** un cliente que ya está en la fila vuelve a ingresar
- **THEN** conserva su posición original

#### Scenario: Ingresos simultáneos
- **WHEN** muchos clientes ingresan a la vez
- **THEN** cada uno obtiene una posición distinta, sin huecos ni repetidos

#### Scenario: Abandono
- **WHEN** un cliente abandona la fila
- **THEN** sale de la fila y los que estaban detrás avanzan un lugar

### Requirement: Habilitación por tandas
El sistema DEBE habilitar a los clientes en tandas de tamaño configurable, cada un intervalo configurable, en
orden estricto de llegada. Cada cliente habilitado DEBE recibir un turno firmado con vencimiento. Aunque haya
varias instancias de ms-fila, cada tanda DEBE habilitarse una sola vez.

#### Scenario: Tanda
- **WHEN** pasa el intervalo configurado
- **THEN** se habilitan los primeros N clientes en espera y cada uno ve el estado HABILITADO con su turno

#### Scenario: Varias instancias
- **WHEN** dos instancias de ms-fila ejecutan el job de tandas a la vez
- **THEN** solo una habilita la tanda y ningún cliente se habilita dos veces

#### Scenario: Configuración
- **WHEN** el organizador del evento (o un ADMIN) cambia tamaño de tanda, intervalo o minutos para comprar
- **THEN** la fila usa la nueva configuración; otro organizador recibe 403
