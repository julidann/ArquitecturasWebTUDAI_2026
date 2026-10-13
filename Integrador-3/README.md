# ENDPOINTS
### a) Dar de alta un estudiante
(POST http://localhost:8080/api/estudiantes)

{
"dni": 123456789,
"nombres": "Julieta",
"apellido": "D'Annunzio",
"edad": 30,
"genero": "Femenino",
"ciudadResidencia": "Tandil",
"lu": 123456
}

### b) Matricular un estudiante en una carrera
(POST http://localhost:8080/api/estudiantes/matricular)

{
"dniEstudiante": 123456789,
"idCarrera": 1,
"antiguedad": 0
}

### c) Recuperar todos los estudiantes, y especificar algún criterio de ordenamiento simple. (por apellido)

(GET http://localhost:8080/api/estudiantes)

### d) Recuperar un estudiante, en base a su número de libreta universitaria.

(GET http://localhost:8080/api/estudiantes/lu/80375)

### e) Recuperar todos los estudiantes, en base a su género.
(GET http://localhost:8080/api/estudiantes/genero/female)

### f) Recuperar las carreras con estudiantes inscriptos, y ordenar por cantidad de inscriptos.

(GET http://localhost:8080/api/estudiantes/carrera/1/ciudad/Rauch)

### g) Recuperar los estudiantes de una determinada carrera, filtrado por ciudad de residencia.

(GET http://localhost:8080/api/estudiantes/carrera/1/ciudad/Rauch)

### h) generar un reporte de las carreras, que para cada carrera incluya información de los inscriptos y egresados por año. Se deben ordenar las carreras alfabéticamente, y presentar los años de manera cronológica

(GET http://localhost:8080/api/carreras/reporte)