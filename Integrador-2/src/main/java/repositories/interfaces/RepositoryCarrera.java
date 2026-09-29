package repositories.interfaces;

import dtos.CarreraDTO;
import dtos.ReporteCarreraDTO;
import entities.Carrera;
import java.util.List;

public interface RepositoryCarrera {
    void save(Carrera carrera);
    CarreraDTO selectById(int id);
    List<CarreraDTO> selectAll();
    boolean delete(int id);
    void matricularEstudianteEnCarrera(Long lu, String nombreCarrera);
    List<CarreraDTO> generarReporteCarreras();
    List<ReporteCarreraDTO> reporteCarreras();
}
