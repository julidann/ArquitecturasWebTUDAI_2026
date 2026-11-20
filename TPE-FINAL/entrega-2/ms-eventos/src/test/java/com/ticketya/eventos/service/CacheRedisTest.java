package com.ticketya.eventos.service;

import com.ticketya.eventos.dto.*;
import com.ticketya.eventos.model.Rol;
import com.ticketya.eventos.model.TipoEvento;
import com.ticketya.eventos.model.TipoSector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cache del catalogo contra un Redis real: se guarda al consultar, se lee del cache (incluida la
 * deserializacion JSON) y se invalida despues del commit cuando el evento cambia.
 */
@SpringBootTest(properties = "spring.cache.type=redis")
@Testcontainers(disabledWithoutDocker = true)
class CacheRedisTest {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired
    EventoService eventoService;
    @Autowired
    LugarService lugarService;
    @Autowired
    BusquedaService busquedaService;
    @Autowired
    StringRedisTemplate template;

    @Test
    void elDetalleDelEventoSeCacheaYSeInvalidaAlModificarlo() {
        LugarResponse lugar = lugarService.crear(new LugarRequest("Sala", "Calle 1", "Tandil", null, null,
                List.of(new SectorRequest("Campo", TipoSector.NO_NUMERADO, 100, null, null))));
        EventoResponse creado = eventoService.crear(new EventoRequest("Show", "d", TipoEvento.RECITAL, List.of("Banda"),
                LocalDateTime.now().plusDays(30), lugar.id(), false, null), 2L, Rol.ORGANIZADOR);

        EventoResponse primera = eventoService.obtener(creado.id());
        assertThat(template.hasKey("ticketya:eventos::" + creado.id())).isTrue();
        assertThat(template.getExpire("ticketya:eventos::" + creado.id())).isBetween(1L, 600L);

        EventoResponse cacheada = eventoService.obtener(creado.id());
        assertThat(cacheada).isEqualTo(primera);

        eventoService.actualizar(creado.id(), new EventoRequest("Show (nuevo nombre)", "d", TipoEvento.RECITAL,
                List.of("Banda"), LocalDateTime.now().plusDays(30), lugar.id(), false, null), 2L, Rol.ORGANIZADOR);
        assertThat(template.hasKey("ticketya:eventos::" + creado.id())).isFalse();
        assertThat(eventoService.obtener(creado.id()).nombre()).isEqualTo("Show (nuevo nombre)");
    }

    @Test
    void lasBusquedasSeCacheanConTtlCorto() {
        FiltroBusqueda filtro = new FiltroBusqueda(null, TipoEvento.TEATRO, null, null, "Azul", null, null,
                OrdenBusqueda.FECHA, false, 0, 10);
        busquedaService.buscar(filtro);
        var claves = template.keys("ticketya:busquedas::*");
        assertThat(claves).isNotEmpty();
        assertThat(template.getExpire(claves.iterator().next())).isBetween(1L, 60L);
        assertThat(busquedaService.buscar(filtro).totalElementos()).isZero();
    }
}
