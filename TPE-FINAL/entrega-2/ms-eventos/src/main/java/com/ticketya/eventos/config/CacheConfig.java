package com.ticketya.eventos.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketya.eventos.dto.EventoResponse;
import com.ticketya.eventos.dto.LugarResponse;
import com.ticketya.eventos.dto.PaginaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.List;

/**
 * Estrategia de cache (cache-aside con Spring Cache sobre Redis, compartido por todas las instancias):
 *
 * | cache      | que guarda                     | TTL    | invalidacion                                  |
 * |------------|--------------------------------|--------|-----------------------------------------------|
 * | eventos    | detalle de un evento (GET /eventos/{id}) | 10 min | se borra la entrada en cada cambio del evento |
 * | busquedas  | paginas de resultados de busqueda | 60 s | se vacia entero ante cualquier cambio de un evento |
 * | lugares    | detalle de un lugar y listado por ciudad | 1 h | los lugares no se modifican; el listado se vacia al crear uno |
 *
 * NO se cachea la disponibilidad de lugares, las reservas, las compras ni la fila virtual: cambian en
 * cada compra y un dato viejo haria ofrecer asientos ya vendidos (ver docs/06).
 *
 * El cache es "transaction aware": las invalidaciones se aplican despues del commit, asi otro request
 * no vuelve a cachear el dato viejo mientras la transaccion sigue abierta. Si Redis se cae, el error
 * se loguea y se sigue sin cache (la fuente de verdad es PostgreSQL).
 */
@Configuration
public class CacheConfig implements CachingConfigurer {

    public static final String EVENTOS = "eventos";
    public static final String BUSQUEDAS = "busquedas";
    public static final String LUGARES = "lugares";
    public static final String LUGARES_POR_CIUDAD = "lugares-ciudad";

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public RedisCacheManagerBuilderCustomizer cachesTicketYa(ObjectMapper mapper) {
        JavaType evento = mapper.constructType(EventoResponse.class);
        JavaType pagina = mapper.getTypeFactory().constructParametricType(PaginaResponse.class, EventoResponse.class);
        JavaType lugar = mapper.constructType(LugarResponse.class);
        JavaType lugares = mapper.getTypeFactory().constructCollectionType(List.class, LugarResponse.class);
        return builder -> builder
                .transactionAware()
                .withCacheConfiguration(EVENTOS, config(mapper, evento, Duration.ofMinutes(10)))
                .withCacheConfiguration(BUSQUEDAS, config(mapper, pagina, Duration.ofSeconds(60)))
                .withCacheConfiguration(LUGARES, config(mapper, lugar, Duration.ofHours(1)))
                .withCacheConfiguration(LUGARES_POR_CIUDAD, config(mapper, lugares, Duration.ofHours(1)));
    }

    private RedisCacheConfiguration config(ObjectMapper mapper, JavaType tipo, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .prefixCacheNameWith("ticketya:")
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new Jackson2JsonRedisSerializer<>(mapper, tipo)));
    }

    /** Redis caido no tiene que tirar abajo el catalogo: se loguea y se va directo a la base. */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache {} no disponible al leer ({}): se consulta la base", cache.getName(), e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Cache {} no disponible al guardar: {}", cache.getName(), e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache {} no disponible al invalidar: {}", cache.getName(), e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Cache {} no disponible al vaciar: {}", cache.getName(), e.getMessage());
            }
        };
    }
}
