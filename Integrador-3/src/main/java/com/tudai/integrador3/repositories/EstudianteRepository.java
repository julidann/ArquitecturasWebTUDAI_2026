package com.tudai.integrador3.repositories;

import com.tudai.integrador3.entities.Estudiante;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Integer> { // Integer porque DNI es el @Id

    // c) Recuperar todos los estudiantes ordenados por algún criterio (ej. apellido asc)
    List<Estudiante> findAllByOrderByApellidoAsc();

    // d) Recuperar un estudiante en base a su número de libreta universitaria (LU)
    Optional<Estudiante> findByLu(Long lu);

    // e) Recuperar todos los estudiantes en base a su género
    List<Estudiante> findByGeneroIgnoreCase(String genero);

    // g) Recuperar estudiantes de una determinada carrera filtrado por ciudad de residencia
    @Query("SELECT i.estudiante FROM Inscripcion i " +
            "WHERE i.carrera.id = :idCarrera AND LOWER(i.estudiante.ciudadResidencia) = LOWER(:ciudad)")
    List<Estudiante> findByCarreraAndCiudad(@Param("idCarrera") int idCarrera, @Param("ciudad") String ciudad);
}
