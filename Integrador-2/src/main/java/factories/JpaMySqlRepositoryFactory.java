package factories;

import jakarta.persistence.*;
import repositories.*;
import repositories.interfaces.*;

public class JpaMySqlRepositoryFactory extends RepositoryFactory {
    private static final String PERSISTENCE_UNIT_NAME = "Integrador 2";
    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);

    @Override
    public RepositoryInscripcion getInscripcionRepository(EntityManager em) {
        return new JpaInscripcionRepository(em);
    }

    @Override
    public RepositoryCarrera getCarreraRepository(EntityManager em) {
        return new JpaCarreraRepository(em);
    }

    @Override
    public RepositoryEstudiante getEstudianteRepository(EntityManager em) {
        return new JpaEstudianteRepository(em);
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }
}