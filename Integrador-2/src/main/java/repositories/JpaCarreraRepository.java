package repositories;

import dtos.CarreraDTO;
import dtos.ReporteCarreraDTO;
import entities.Carrera;
import entities.Estudiante;
import entities.Inscripcion;
import jakarta.persistence.EntityManager;
import repositories.interfaces.RepositoryCarrera;

import java.util.ArrayList;
import java.util.List;

public class JpaCarreraRepository implements RepositoryCarrera {

    private final EntityManager em;

    public JpaCarreraRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public void save(Carrera carrera) {
        try {
            em.getTransaction().begin();
            em.persist(carrera);
            em.getTransaction().commit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public CarreraDTO selectById(int id) {
        CarreraDTO resultado = null;

        try {
            Carrera carrera = em.find(Carrera.class, id);

            if (carrera != null) {
                resultado = new CarreraDTO(carrera.getNombre());

                for (Inscripcion inscripcion : carrera.getInscripciones()) {
                    resultado.addInscripcion(inscripcion);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultado;
    }

    @Override
    public List<CarreraDTO> selectAll() {
        List<CarreraDTO> resultado = new ArrayList<>();

        try {
            List<Carrera> carreras = em.createQuery(
                    "SELECT c FROM Carrera c ORDER BY c.nombre ASC",
                    Carrera.class
            ).getResultList();

            for (Carrera carrera : carreras) {
                CarreraDTO dto = new CarreraDTO(carrera.getNombre());

                for (Inscripcion inscripcion : carrera.getInscripciones()) {
                    dto.addInscripcion(inscripcion);
                }

                resultado.add(dto);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultado;
    }

    @Override
    public boolean delete(int id) {
        boolean eliminado = false;

        try {
            Carrera carrera = em.find(Carrera.class, id);

            if (carrera != null) {
                em.getTransaction().begin();
                em.remove(carrera);
                em.getTransaction().commit();
                eliminado = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return eliminado;
    }

    @Override
    public void matricularEstudianteEnCarrera(Long lu, String nombreCarrera) {
        try {
            Estudiante estudiante = em.createQuery(
                            "SELECT e FROM Estudiante e WHERE e.lu = :lu",
                            Estudiante.class
                    )
                    .setParameter("lu", lu)
                    .getSingleResult();

            Carrera carrera = em.createQuery(
                            "SELECT c FROM Carrera c WHERE c.nombre = :nombre",
                            Carrera.class
                    )
                    .setParameter("nombre", nombreCarrera)
                    .getSingleResult();

            em.getTransaction().begin();

            em.persist(new Inscripcion(carrera, estudiante));

            em.getTransaction().commit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<CarreraDTO> generarReporteCarreras() {
        List<CarreraDTO> resultado = new ArrayList<>();

        try {
            List<Carrera> carreras = em.createQuery(
                    "SELECT DISTINCT c " +
                            "FROM Carrera c " +
                            "LEFT JOIN FETCH c.inscripciones " +
                            "ORDER BY c.nombre ASC",
                    Carrera.class
            ).getResultList();

            for (Carrera carrera : carreras) {

                CarreraDTO dto = new CarreraDTO(carrera.getNombre());

                for (Inscripcion inscripcion : carrera.getInscripciones()) {
                    dto.addInscripcion(inscripcion);
                }

                resultado.add(dto);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultado;
    }

    @Override
    public List<ReporteCarreraDTO> reporteCarreras() {

        List<ReporteCarreraDTO> reporte = new ArrayList<>();

        try {

            // INSCRIPTOS POR CARRERA Y AÑO
            List<Object[]> inscriptos = em.createQuery(
                    "SELECT c.nombre, YEAR(i.anioInscripcion), COUNT(i) " +
                            "FROM Carrera c " +
                            "JOIN c.inscripciones i " +
                            "WHERE i.anioInscripcion IS NOT NULL " +
                            "GROUP BY c.nombre, YEAR(i.anioInscripcion) " +
                            "ORDER BY c.nombre ASC, YEAR(i.anioInscripcion) ASC",
                    Object[].class
            ).getResultList();

            // EGRESADOS POR CARRERA Y AÑO
            List<Object[]> egresados = em.createQuery(
                    "SELECT c.nombre, YEAR(i.anioEgreso), COUNT(i) " +
                            "FROM Carrera c " +
                            "JOIN c.inscripciones i " +
                            "WHERE i.graduado = true " +
                            "AND i.anioEgreso IS NOT NULL " +
                            "GROUP BY c.nombre, YEAR(i.anioEgreso) " +
                            "ORDER BY c.nombre ASC, YEAR(i.anioEgreso) ASC",
                    Object[].class
            ).getResultList();

            // CARGAMOS LOS INSCRIPTOS
            for (Object[] fila : inscriptos) {

                String carrera = (String) fila[0];
                int anio = ((Number) fila[1]).intValue();
                long cantidadInscriptos = ((Number) fila[2]).longValue();

                long cantidadEgresados = 0;

                // Buscamos si existe un registro de egresados
                // para la misma carrera y año
                for (Object[] egresado : egresados) {

                    String carreraEgresado = (String) egresado[0];
                    int anioEgresado = ((Number) egresado[1]).intValue();

                    if (carrera.equals(carreraEgresado)
                            && anio == anioEgresado) {

                        cantidadEgresados =
                                ((Number) egresado[2]).longValue();

                        break;
                    }
                }

                reporte.add(new ReporteCarreraDTO(
                        carrera,
                        anio,
                        cantidadInscriptos,
                        cantidadEgresados
                ));
            }

            // AGREGAMOS AÑOS QUE SOLO TENGAN EGRESADOS
            for (Object[] egresado : egresados) {

                String carrera = (String) egresado[0];
                int anio = ((Number) egresado[1]).intValue();
                long cantidadEgresados = ((Number) egresado[2]).longValue();

                boolean existe = false;

                for (ReporteCarreraDTO dto : reporte) {

                    if (dto.getCarrera().equals(carrera)
                            && dto.getAnio() == anio) {

                        existe = true;
                        break;
                    }
                }

                if (!existe) {
                    reporte.add(new ReporteCarreraDTO(
                            carrera,
                            anio,
                            0,
                            cantidadEgresados
                    ));
                }
            }

            // ORDEN FINAL: CARRERA Y DESPUÉS AÑO
            reporte.sort((a, b) -> {

                int comparacionCarrera =
                        a.getCarrera().compareTo(b.getCarrera());

                if (comparacionCarrera != 0) {
                    return comparacionCarrera;
                }

                return Integer.compare(
                        a.getAnio(),
                        b.getAnio()
                );
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        return reporte;
    }
}