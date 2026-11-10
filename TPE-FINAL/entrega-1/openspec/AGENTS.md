# Instrucciones para asistentes de IA

Si usás un coding agent en este repo:

1. Antes de programar, leé `openspec/project.md` y las specs de las capacidades afectadas en `openspec/specs/`.
2. Todo cambio de comportamiento empieza con un change en `openspec/changes/<id>/`
   (`proposal.md`, `specs/<capacidad>/spec.md` con `## ADDED|MODIFIED|REMOVED Requirements`,
   `design.md` si hace falta, y `tasks.md`). No implementar hasta que el grupo lo apruebe.
3. Cada requisito tiene al menos un `#### Scenario:` con **WHEN**/**THEN**. Cada escenario debería
   tener una prueba automática que lo cubra.
4. Al terminar, marcar las tareas con `[x]` y archivar el change en `changes/archive/AAAA-MM-DD-<id>/`.
5. Respetar las convenciones de `project.md` (español, un proyecto por microservicio, sin locks en memoria
   para el control de concurrencia).
