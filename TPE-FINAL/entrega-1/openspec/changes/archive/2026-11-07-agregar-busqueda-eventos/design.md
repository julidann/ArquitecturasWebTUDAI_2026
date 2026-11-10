# Diseño: búsqueda

## Decisiones
- **JPA Specifications**: cada filtro es una `Specification` que devuelve `null` si el parámetro no vino, y se
  combinan con `and`. Evita escribir una query por combinación.
- **Precio** = precio mínimo del evento (columna desnormalizada).
- **Relevancia simple**: puntaje por coincidencia de cada término del texto (3 en nombre, 2 en participantes,
  1 en descripción, +1 si el evento es en los próximos 30 días), sin tildes ni mayúsculas. Se calcula en memoria
  sobre los eventos que pasaron los filtros; para el volumen del TP alcanza. Si creciera, la alternativa es un
  índice de texto (PostgreSQL full-text o Elasticsearch).
- **Tamaño de página máximo 50** para que nadie pida todo el catálogo de una.
- En la entrega 2 los resultados de búsqueda se cachean (ver consigna); el diseño ya separa la búsqueda en su
  propio service para poder agregarle `@Cacheable`.
