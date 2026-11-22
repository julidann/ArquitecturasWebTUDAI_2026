package com.ticketya.fila;

import com.ticketya.fila.dto.EstadoTurno;
import com.ticketya.fila.dto.TurnoResponse;
import com.ticketya.fila.exception.OperacionInvalidaException;
import com.ticketya.fila.exception.RecursoNoEncontradoException;
import com.ticketya.fila.service.FilaService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba la fila contra un Redis real (Testcontainers). Se saltea si no hay Docker. */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class FilaServiceTest {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired
    FilaService fila;
    @Autowired
    StringRedisTemplate template;

    static final long EVENTO = 7;

    @BeforeEach
    void limpiar() {
        template.getConnectionFactory().getConnection().serverCommands().flushAll();
        fila.abrir(EVENTO, 2L); // tanda de 2 (application.yml de test)
    }

    @Test
    void losClientesQuedanEnOrdenDeLlegadaYVenSuPosicion() {
        assertThat(fila.ingresar(EVENTO, 100).posicion()).isEqualTo(1);
        assertThat(fila.ingresar(EVENTO, 101).posicion()).isEqualTo(2);
        TurnoResponse t = fila.ingresar(EVENTO, 102);
        assertThat(t.estado()).isEqualTo(EstadoTurno.EN_ESPERA);
        assertThat(t.posicion()).isEqualTo(3);
        assertThat(t.enEspera()).isEqualTo(3);
        // con tandas de 2 cada 1 s, el tercero espera 2 tandas
        assertThat(t.esperaEstimadaSegundos()).isEqualTo(2);
    }

    @Test
    void reingresarNoHacePerderNiGanarLugares() {
        fila.ingresar(EVENTO, 100);
        fila.ingresar(EVENTO, 101);
        assertThat(fila.ingresar(EVENTO, 100).posicion()).isEqualTo(1);
        assertThat(fila.miTurno(EVENTO, 101).posicion()).isEqualTo(2);
    }

    @Test
    void cadaTandaHabilitaALosPrimerosYLesDaUnTurnoFirmado() {
        fila.ingresar(EVENTO, 100);
        fila.ingresar(EVENTO, 101);
        fila.ingresar(EVENTO, 102);

        assertThat(fila.habilitarTanda(EVENTO)).isEqualTo(2);

        TurnoResponse primero = fila.miTurno(EVENTO, 100);
        assertThat(primero.estado()).isEqualTo(EstadoTurno.HABILITADO);
        assertThat(primero.turno()).isNotBlank();
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor("clave-de-test-de-la-fila-con-mas-de-32-bytes".getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(primero.turno()).getPayload();
        assertThat(claims.getSubject()).isEqualTo("100");
        assertThat(claims.get("eventoId", Long.class)).isEqualTo(EVENTO);

        assertThat(fila.miTurno(EVENTO, 101).estado()).isEqualTo(EstadoTurno.HABILITADO);
        TurnoResponse tercero = fila.miTurno(EVENTO, 102);
        assertThat(tercero.estado()).isEqualTo(EstadoTurno.EN_ESPERA);
        assertThat(tercero.posicion()).isEqualTo(1);
    }

    @Test
    void noSeHabilitaOtraTandaHastaQuePaseElIntervalo() throws Exception {
        for (long u = 100; u < 106; u++) {
            fila.ingresar(EVENTO, u);
        }
        assertThat(fila.habilitarTanda(EVENTO)).isEqualTo(2);
        assertThat(fila.habilitarTanda(EVENTO)).isZero(); // mismo intervalo
        Thread.sleep(1100);
        assertThat(fila.habilitarTanda(EVENTO)).isEqualTo(2);
    }

    @Test
    void conVariasInstanciasCorriendoElJobSoloUnaHabilitaCadaTandaYNadieSeHabilitaDosVeces() throws Exception {
        for (long u = 1; u <= 50; u++) {
            fila.ingresar(EVENTO, u);
        }
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            resultados.add(pool.submit(() -> fila.habilitarTanda(EVENTO)));
        }
        int total = 0;
        for (Future<Integer> f : resultados) {
            total += f.get();
        }
        pool.shutdown();
        assertThat(total).isEqualTo(2);
        assertThat(fila.miTurno(EVENTO, 3).posicion()).isEqualTo(1);
    }

    @Test
    void ingresosSimultaneosRespetanElOrdenSinRepetirPosiciones() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Future<TurnoResponse>> futuros = new ArrayList<>();
        for (long u = 1; u <= 200; u++) {
            long usuario = u;
            futuros.add(pool.submit(() -> fila.ingresar(EVENTO, usuario)));
        }
        Set<Long> posiciones = new HashSet<>();
        for (Future<TurnoResponse> f : futuros) {
            f.get();
        }
        for (long u = 1; u <= 200; u++) {
            posiciones.add(fila.miTurno(EVENTO, u).posicion());
        }
        pool.shutdown();
        assertThat(posiciones).hasSize(200);
        assertThat(Collections.max(posiciones)).isEqualTo(200);
    }

    @Test
    void unEventoSinFilaDa404YUnaFilaCerradaDa409() {
        assertThatThrownBy(() -> fila.ingresar(999, 100)).isInstanceOf(RecursoNoEncontradoException.class);
        fila.cerrar(EVENTO);
        assertThatThrownBy(() -> fila.ingresar(EVENTO, 100)).isInstanceOf(OperacionInvalidaException.class);
        assertThat(fila.filasActivas()).doesNotContain(EVENTO);
    }

    @Test
    void abandonarLaFilaLiberaElLugar() {
        fila.ingresar(EVENTO, 100);
        fila.ingresar(EVENTO, 101);
        fila.abandonar(EVENTO, 100);
        assertThat(fila.miTurno(EVENTO, 101).posicion()).isEqualTo(1);
        assertThatThrownBy(() -> fila.miTurno(EVENTO, 100)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
