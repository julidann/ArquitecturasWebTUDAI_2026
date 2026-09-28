package factories;

import jakarta.persistence.EntityManager;
import repositories.interfaces.RepositoryCarrera;

public abstract class RepositoryFactory {
    public static final int MYSQL_JDBC = 1;

    public abstract RepositoryCarrera getCarreraRepository(EntityManager em);
    public abstract RepositoryEstudiante getEstudianteRepository(EntityManager em);
    public abstract RepositoryInscripcion getInscripcionRepository(EntityManager em);

    public static RepositoryFactory getDAOFactory(int whichFactory) {
        if (whichFactory == MYSQL_JDBC) {
            return new JpaMySqlRepositoryFactory();
        }
        throw new IllegalArgumentException("Factory no soportada: " + whichFactory);
    }
}