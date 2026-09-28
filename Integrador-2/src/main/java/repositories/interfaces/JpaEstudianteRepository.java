package repositories;

import dtos.EstudianteDTO;
import entities.Estudiante;
import jakarta.persistence.*;
import repositories.interfaces.RepositoryEstudiante;
import java.util.ArrayList;
import java.util.List;

public class JpaEstudianteRepository implements RepositoryEstudiante {
    private final EntityManager em;

    public JpaEstudianteRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public void save(Estudiante estudiante) {
        try {
            em.getTransaction().begin();
            em.persist(estudiante);
            em.getTransaction().commit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public EstudianteDTO selectById(int dni) {
        EstudianteDTO estudiante = null;

        try {
            List<EstudianteDTO> resultado = em.createQuery(
                    "SELECT new dtos.EstudianteDTO(e.nombres,e.apellido,e.edad,e.genero,e.dni,e.ciudadResidencia,e.lu) " +
                    "FROM Estudiante e WHERE e.dni = :dni",
                    EstudianteDTO.class)
                    .setParameter("dni", dni)
                    .getResultList();

            if (!resultado.isEmpty()) {
                estudiante = resultado.get(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return estudiante;
    }

    @Override
    public List<EstudianteDTO> selectAll() {
        return obtenerEstudiantesOrdenadosPorNombre();
    }

    @Override
    public boolean delete(int dni) {
        boolean eliminado = false;

        try {
            Estudiante estudiante = em.find(Estudiante.class, dni);

            if (estudiante != null) {
                em.getTransaction().begin();
                em.remove(estudiante);
                em.getTransaction().commit();
                eliminado = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return eliminado;
    }

    @Override
    public List<EstudianteDTO> obtenerEstudiantesOrdenadosPorNombre() {
        List<EstudianteDTO> estudiantes = new ArrayList<>();

        try {
            estudiantes = em.createQuery(
                    "SELECT new dtos.EstudianteDTO(e.nombres,e.apellido,e.edad,e.genero,e.dni,e.ciudadResidencia,e.lu) " +
                    "FROM Estudiante e ORDER BY e.nombres ASC",
                    EstudianteDTO.class
            ).getResultList();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return estudiantes;
    }

    @Override
    public EstudianteDTO buscarPorLibreta(Long lu) {
        EstudianteDTO estudiante = null;

        try {
            List<EstudianteDTO> resultado = em.createQuery(
                    "SELECT new dtos.EstudianteDTO(e.nombres,e.apellido,e.edad,e.genero,e.dni,e.ciudadResidencia,e.lu) " +
                    "FROM Estudiante e WHERE e.lu = :lu",
                    EstudianteDTO.class)
                    .setParameter("lu", lu)
                    .getResultList();

            if (!resultado.isEmpty()) {
                estudiante = resultado.get(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return estudiante;
    }

    @Override
    public List<EstudianteDTO> buscarPorGenero(String genero) {
        List<EstudianteDTO> estudiantes = new ArrayList<>();

        try {
            estudiantes = em.createQuery(
                    "SELECT new dtos.EstudianteDTO(e.nombres,e.apellido,e.edad,e.genero,e.dni,e.ciudadResidencia,e.lu) " +
                    "FROM Estudiante e WHERE e.genero = :genero ORDER BY e.apellido ASC, e.nombres ASC",
                    EstudianteDTO.class)
                    .setParameter("genero", genero)
                    .getResultList();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return estudiantes;
    }
}
