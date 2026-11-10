package com.ticketya.reservas.concurrencia;

import com.ticketya.reservas.MutableClock;
import com.ticketya.reservas.client.EventosClient;
import com.ticketya.reservas.dto.DisponibilidadInicialRequest;
import com.ticketya.reservas.dto.DisponibilidadInicialRequest.AsientoInicial;
import com.ticketya.reservas.dto.DisponibilidadInicialRequest.SectorInicial;
import com.ticketya.reservas.dto.ReservaRequest;
import com.ticketya.reservas.dto.ReservaResponse;
import com.ticketya.reservas.exception.LugarNoDisponibleException;
import com.ticketya.reservas.model.EstadoAsiento;
import com.ticketya.reservas.model.EstadoReserva;
import com.ticketya.reservas.model.TipoSector;
import com.ticketya.reservas.repository.AsientoDisponibilidadRepository;
import com.ticketya.reservas.repository.SectorDisponibilidadRepository;
import com.ticketya.reservas.service.DisponibilidadService;
import com.ticketya.reservas.service.ReservaService;
import com.ticketya.reservas.service.VencimientoReservasJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.ConcurrencyFailureException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de la funcionalidad 5: "una entrada nunca puede venderse dos veces".
 *
 * Se lanzan muchos hilos a la vez (cada uno simula un cliente distinto) que intentan
 * reservar el mismo asiento o el ultimo lugar de una zona, y se verifica que solo uno
 * lo logre. Las pruebas corren contra la base real (no mocks) porque el mecanismo vive
 * justamente en la base de datos. Esta clase base se ejecuta con H2 y con PostgreSQL.
 */
abstract class NoDobleVentaTestBase {

    protected static final int CLIENTES_SIMULTANEOS = 100;
    private static final AtomicLong SECUENCIA_EVENTOS = new AtomicLong(1000);

    @Autowired
    protected ReservaService reservaService;
    @Autowired
    protected DisponibilidadService disponibilidadService;
    @Autowired
    protected AsientoDisponibilidadRepository asientoRepository;
    @Autowired
    protected SectorDisponibilidadRepository sectorRepository;
    @Autowired
    protected VencimientoReservasJob vencimientoJob;
    @Autowired
    protected MutableClock clock;

    @MockBean
    protected EventosClient eventosClient;

    @AfterEach
    void resetearReloj() {
        clock.resetear();
    }

    /** Crea un evento con una platea numerada de 2 filas x 5 asientos (ids 1..10) y un campo de capacidad dada. */
    protected long publicarEvento(int capacidadCampo) {
        long eventoId = SECUENCIA_EVENTOS.incrementAndGet();
        List<AsientoInicial> asientos = new ArrayList<>();
        long id = 1;
        for (String fila : List.of("A", "B")) {
            for (int n = 1; n <= 5; n++) {
                asientos.add(new AsientoInicial(id++, fila, n));
            }
        }
        disponibilidadService.inicializar(new DisponibilidadInicialRequest(eventoId, "Evento " + eventoId,
                LocalDateTime.now().plusDays(30), 6, List.of(
                new SectorInicial(1L, "Platea", TipoSector.NUMERADO, new BigDecimal("50000"), 10, asientos),
                new SectorInicial(2L, "Campo", TipoSector.NO_NUMERADO, new BigDecimal("30000"), capacidadCampo, null))));
        return eventoId;
    }

    /**
     * Ejecuta el mismo pedido desde N clientes distintos arrancando todos a la vez
     * (CountDownLatch) y devuelve cuantos tuvieron exito.
     */
    protected Resultado ejecutarEnParalelo(int clientes, Function<Long, ReservaResponse> pedido) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger rechazos = new AtomicInteger();
        List<Throwable> inesperados = new CopyOnWriteArrayList<>();
        List<Future<?>> futuros = new ArrayList<>();
        for (int i = 0; i < clientes; i++) {
            long usuarioId = 10_000L + i;
            futuros.add(pool.submit(() -> {
                largada.await();
                try {
                    pedido.apply(usuarioId);
                    exitos.incrementAndGet();
                } catch (LugarNoDisponibleException | ConcurrencyFailureException e) {
                    rechazos.incrementAndGet();
                } catch (Throwable t) {
                    inesperados.add(t);
                }
                return null;
            }));
        }
        largada.countDown();
        for (Future<?> f : futuros) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();
        assertThat(inesperados).as("errores inesperados").isEmpty();
        return new Resultado(exitos.get(), rechazos.get());
    }

    protected record Resultado(int exitos, int rechazos) {
    }

    @Test
    void cienClientesQuierenElMismoAsientoYSoloUnoLoConsigue() throws Exception {
        long eventoId = publicarEvento(100);

        Resultado r = ejecutarEnParalelo(CLIENTES_SIMULTANEOS,
                usuario -> reservaService.crear(usuario, new ReservaRequest(eventoId, 1L, List.of(3L), null)));

        assertThat(r.exitos()).isEqualTo(1);
        assertThat(r.rechazos()).isEqualTo(CLIENTES_SIMULTANEOS - 1);
        var asiento = asientoRepository.findByEventoIdAndAsientoIdIn(eventoId, List.of(3L)).get(0);
        assertThat(asiento.getEstado()).isEqualTo(EstadoAsiento.RESERVADO);
        assertThat(sectorRepository.findByEventoIdAndSectorId(eventoId, 1L).orElseThrow().getDisponibles())
                .isEqualTo(9);
    }

    @Test
    void cienClientesQuierenElUltimoLugarDeUnaZonaYSoloUnoLoConsigue() throws Exception {
        long eventoId = publicarEvento(1);

        Resultado r = ejecutarEnParalelo(CLIENTES_SIMULTANEOS,
                usuario -> reservaService.crear(usuario, new ReservaRequest(eventoId, 2L, null, 1)));

        assertThat(r.exitos()).isEqualTo(1);
        assertThat(sectorRepository.findByEventoIdAndSectorId(eventoId, 2L).orElseThrow().getDisponibles())
                .isZero();
    }

    @Test
    void laZonaNuncaSeSobrevende() throws Exception {
        long eventoId = publicarEvento(25);

        // 100 clientes piden 2 lugares cada uno en una zona de 25: entran 12 (24 lugares) y queda 1 libre
        Resultado r = ejecutarEnParalelo(CLIENTES_SIMULTANEOS,
                usuario -> reservaService.crear(usuario, new ReservaRequest(eventoId, 2L, null, 2)));

        assertThat(r.exitos()).isEqualTo(12);
        assertThat(sectorRepository.findByEventoIdAndSectorId(eventoId, 2L).orElseThrow().getDisponibles())
                .isEqualTo(1);
    }

    @Test
    void pedidosConAsientosSuperpuestosSonTodoONada() throws Exception {
        long eventoId = publicarEvento(10);

        // la mitad pide A1-A2-A3 y la otra mitad A3-A4-A5: comparten el A3, asi que gana un solo pedido
        Resultado r = ejecutarEnParalelo(CLIENTES_SIMULTANEOS, usuario -> {
            List<Long> asientos = usuario % 2 == 0 ? List.of(1L, 2L, 3L) : List.of(3L, 4L, 5L);
            return reservaService.crear(usuario, new ReservaRequest(eventoId, 1L, asientos, null));
        });

        assertThat(r.exitos()).isEqualTo(1);
        long reservados = asientoRepository.findByEventoIdAndSectorIdOrderByFilaAscNumeroAsc(eventoId, 1L).stream()
                .filter(a -> a.getEstado() == EstadoAsiento.RESERVADO)
                .count();
        assertThat(reservados).as("no quedan asientos 'huerfanos' reservados por pedidos fallidos").isEqualTo(3);
    }

    @Test
    void unaReservaVencidaLiberaElAsientoYNoSePuedeConfirmar() {
        long eventoId = publicarEvento(10);
        ReservaResponse primera = reservaService.crear(1L, new ReservaRequest(eventoId, 1L, List.of(7L), null));

        clock.adelantar(Duration.ofMinutes(11));
        vencimientoJob.liberarVencidas();

        assertThat(reservaService.obtener(primera.id()).estado()).isEqualTo(EstadoReserva.VENCIDA);
        assertThatThrownBy(() -> reservaService.confirmar(primera.id()))
                .hasMessageContaining("VENCIDA");
        // el asiento vuelve a estar disponible para otro cliente
        ReservaResponse segunda = reservaService.crear(2L, new ReservaRequest(eventoId, 1L, List.of(7L), null));
        assertThat(segunda.estado()).isEqualTo(EstadoReserva.PENDIENTE);
    }

    @Test
    void confirmarYVencerALaVezNuncaDejaElAsientoVendidoYLibre() throws Exception {
        long eventoId = publicarEvento(10);
        ReservaResponse reserva = reservaService.crear(1L, new ReservaRequest(eventoId, 1L, List.of(8L), null));
        clock.adelantar(Duration.ofMinutes(9).plusSeconds(59));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        Future<?> confirmacion = pool.submit(() -> {
            largada.await();
            try {
                reservaService.confirmar(reserva.id());
            } catch (RuntimeException ignored) {
                // puede perder contra el vencimiento
            }
            return null;
        });
        Future<?> vencimiento = pool.submit(() -> {
            largada.await();
            clock.adelantar(Duration.ofSeconds(2));
            vencimientoJob.liberarVencidas();
            return null;
        });
        largada.countDown();
        confirmacion.get(30, TimeUnit.SECONDS);
        vencimiento.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        EstadoReserva estado = reservaService.obtener(reserva.id()).estado();
        EstadoAsiento asiento = asientoRepository.findByEventoIdAndAsientoIdIn(eventoId, List.of(8L)).get(0).getEstado();
        // gane quien gane, el estado de la reserva y el del asiento tienen que ser coherentes
        if (estado == EstadoReserva.CONFIRMADA) {
            assertThat(asiento).isEqualTo(EstadoAsiento.VENDIDO);
        } else {
            assertThat(estado).isEqualTo(EstadoReserva.VENCIDA);
            assertThat(asiento).isEqualTo(EstadoAsiento.DISPONIBLE);
        }
    }
}
