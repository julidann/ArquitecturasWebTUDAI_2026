package repositories;

import dtos.CarreraConCantInscriptosDTO;
import dtos.InscripcionDTO;
import entities.Inscripcion;
import jakarta.persistence.*;
import repositories.interfaces.RepositoryInscripcion;
import java.util.ArrayList;
import java.util.List;

public class JpaInscripcionRepository implements RepositoryInscripcion {
    private final EntityManager em;

    public JpaInscripcionRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public void save(Inscripcion inscripcion) {
        try {
            em.getTransaction().begin();
            em.persist(inscripcion);
            em.getTransaction().commit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public InscripcionDTO selectById(int id) {
        InscripcionDTO inscripcion = null;

        try {
            List<InscripcionDTO> resultado = em.createQuery(
                    "SELECT new dtos.InscripcionDTO(i.antiguedad,i.anioInscripcion,i.anioEgreso," +
                    "i.graduado,c.nombre,e.lu) FROM Inscripcion i " +
                    "JOIN i.carrera c JOIN i.estudiante e WHERE i.id = :id",
                    InscripcionDTO.class)
                    .setParameter("id", id)
                    .getResultList();

            if (!resultado.isEmpty()) {
                inscripcion = resultado.get(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return inscripcion;
    }

    @Override
    public List<InscripcionDTO> selectAll() {
        List<InscripcionDTO> inscripciones = new ArrayList<>();

        try {
            inscripciones = em.createQuery(
                    "SELECT new dtos.InscripcionDTO(i.antiguedad,i.anioInscripcion,i.anioEgreso," +
                    "i.graduado,c.nombre,e.lu) FROM Inscripcion i " +
                    "JOIN i.carrera c JOIN i.estudiante e ORDER BY c.nombre, e.lu",
                    InscripcionDTO.class
            ).getResultList();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return inscripciones;
    }

    @Override
    public boolean delete(int id) {
        boolean eliminado = false;

        try {
            Inscripcion inscripcion = em.find(Inscripcion.class, id);

            if (inscripcion != null) {
                em.getTransaction().begin();
                em.remove(inscripcion);
                em.getTransaction().commit();
                eliminado = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return eliminado;
    }

    @Override
    public List<CarreraConCantInscriptosDTO> recuperarCarrerasOrdenadasPorCantidadInscriptos() {
        List<CarreraConCantInscriptosDTO> carreras = new ArrayList<>();

        try {
            carreras = em.createQuery(
                    "SELECT new dtos.CarreraConCantInscriptosDTO(c.nombre, COUNT(i)) " +
                    "FROM Carrera c JOIN c.inscripciones i " +
                    "GROUP BY c.id, c.nombre ORDER BY COUNT(i) DESC, c.nombre ASC",
                    CarreraConCantInscriptosDTO.class
            ).getResultList();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return carreras;
    }

    @Override
    public List<InscripcionDTO> estudiantesDeCarreraPorCiudad(String nombreCarrera, String ciudad) {
        List<InscripcionDTO> estudiantes = new ArrayList<>();

        try {
            estudiantes = em.createQuery(
                    "SELECT new dtos.InscripcionDTO(i.antiguedad,i.anioInscripcion,i.anioEgreso," +
                    "i.graduado,c.nombre,e.lu) " +
                    "FROM Inscripcion i JOIN i.carrera c JOIN i.estudiante e " +
                    "WHERE c.nombre = :carrera AND e.ciudadResidencia = :ciudad " +
                    "ORDER BY e.apellido ASC, e.nombres ASC",
                    InscripcionDTO.class)
                    .setParameter("carrera", nombreCarrera)
                    .setParameter("ciudad", ciudad)
                    .getResultList();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return estudiantes;
    }
}
