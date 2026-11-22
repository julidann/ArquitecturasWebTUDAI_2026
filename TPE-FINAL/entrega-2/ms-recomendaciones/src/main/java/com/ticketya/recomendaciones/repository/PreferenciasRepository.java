package com.ticketya.recomendaciones.repository;

import com.ticketya.recomendaciones.model.Preferencias;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;

public interface PreferenciasRepository extends MongoRepository<Preferencias, Long> {

    /** Clientes con alguno de esos artistas como favorito y que viven en esa ciudad. */
    @Query(value = "{ 'artistasNormalizados': { $in: ?0 }, 'ciudadNormalizada': ?1 }", fields = "{ '_id': 1 }")
    List<Preferencias> interesados(Collection<String> artistas, String ciudad);
}
