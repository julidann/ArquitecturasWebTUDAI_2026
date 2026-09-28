package repositories.interfaces;

import dtos.EstudianteDTO;
import entities.Estudiante;
import java.util.List;

public interface RepositoryEstudiante {
    void save(Estudiante estudiante);
    EstudianteDTO selectById(int dni);
    List<EstudianteDTO> selectAll();
    boolean delete(int dni);
    List<EstudianteDTO> obtenerEstudiantesOrdenadosPorNombre();
    EstudianteDTO buscarPorLibreta(Long lu);
    List<EstudianteDTO> buscarPorGenero(String genero);
}
