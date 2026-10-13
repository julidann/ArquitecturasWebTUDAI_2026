package com.tudai.integrador3.repositories;

import com.tudai.integrador3.entities.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Integer> {

    // Verificar si un estudiante ya está matriculado en una carrera
    Optional<Inscripcion> findByEstudianteDniAndCarreraId(int dni, int idCarrera);
}
