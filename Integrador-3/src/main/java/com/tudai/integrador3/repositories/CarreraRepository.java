package com.tudai.integrador3.repositories;

import com.tudai.integrador3.dtos.CarreraConCantInscriptosDTO;
import com.tudai.integrador3.entities.Carrera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarreraRepository extends JpaRepository<Carrera, Integer> {

    // f) Recuperar las carreras con estudiantes inscriptos, y ordenar por cantidad de inscriptos
    @Query("SELECT new com.tudai.integrador3.dtos.CarreraConCantInscriptosDTO(c.id, c.nombre, COUNT(i.estudiante)) " +
            "FROM Inscripcion i JOIN i.carrera c " +
            "GROUP BY c.id, c.nombre " +
            "HAVING COUNT(i.estudiante) > 0 " +
            "ORDER BY COUNT(i.estudiante) DESC")
    List<CarreraConCantInscriptosDTO> findCarrerasConInscriptosOrdenadas();

    // h) Datos para el reporte de carreras (inscriptos y egresados agrupados por año)
    @Query("SELECT c.nombre, YEAR(i.anioInscripcion), COUNT(i.id), " +
            "SUM(CASE WHEN i.graduado = true THEN 1 ELSE 0 END) " +
            "FROM Inscripcion i JOIN i.carrera c " +
            "GROUP BY c.nombre, YEAR(i.anioInscripcion) " +
            "ORDER BY c.nombre ASC, YEAR(i.anioInscripcion) ASC")
    List<Object[]> findReporteRaw();
}