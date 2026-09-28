package repositories.interfaces;

import dtos.CarreraConCantInscriptosDTO;
import dtos.InscripcionDTO;
import java.util.List;

public interface RepositoryInscripcion {
    void save(entities.Inscripcion inscripcion);
    InscripcionDTO selectById(int id);
    List<InscripcionDTO> selectAll();
    boolean delete(int id);
    List<CarreraConCantInscriptosDTO> recuperarCarrerasOrdenadasPorCantidadInscriptos();
    List<InscripcionDTO> estudiantesDeCarreraPorCiudad(String nombreCarrera, String ciudad);
}
