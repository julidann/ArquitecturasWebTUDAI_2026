package com.ticketya.reservas.service;

import com.ticketya.reservas.client.EventosClient;
import com.ticketya.reservas.dto.ReservaRequest;
import com.ticketya.reservas.exception.LimiteExcedidoException;
import com.ticketya.reservas.exception.OperacionInvalidaException;
import com.ticketya.reservas.model.*;
import com.ticketya.reservas.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Reglas de negocio de la reserva, con los repositorios mockeados. */
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 20, 18, 0);

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private EventoDisponibilidadRepository eventoRepository;
    @Mock
    private SectorDisponibilidadRepository sectorRepository;
    @Mock
    private AsientoDisponibilidadRepository asientoRepository;
    @Mock
    private LiberacionRepository liberacionRepository;
    @Mock
    private EventosClient eventosClient;

    private ReservaService service;
    private EventoDisponibilidad evento;
    private SectorDisponibilidad campo;
    private SectorDisponibilidad platea;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new ReservaService(reservaRepository, eventoRepository, sectorRepository, asientoRepository,
                liberacionRepository, eventosClient, clock, 10);
        evento = new EventoDisponibilidad(1L, "Recital", AHORA.plusDays(30), 4);
        campo = new SectorDisponibilidad(1L, 2L, "Campo", TipoSector.NO_NUMERADO, new BigDecimal("30000"), 100);
        platea = new SectorDisponibilidad(1L, 1L, "Platea", TipoSector.NUMERADO, new BigDecimal("50000"), 6);
    }

    @Test
    void reservaEnZonaNoNumeradaCalculaMontoYVencimiento() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(sectorRepository.findByEventoIdAndSectorId(1L, 2L)).thenReturn(Optional.of(campo));
        when(sectorRepository.descontar(1L, 2L, 3)).thenReturn(1);
        when(reservaRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setId(50L);
            return r;
        });

        var resp = service.crear(100L, new ReservaRequest(1L, 2L, null, 3));

        assertThat(resp.estado()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(resp.montoTotal()).isEqualByComparingTo("90000");
        assertThat(resp.expiraEn()).isEqualTo(AHORA.plusMinutes(10));
        assertThat(resp.items()).hasSize(3);
    }

    @Test
    void noSePuedeSuperarElLimiteDeEntradasPorCliente() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(sectorRepository.findByEventoIdAndSectorId(1L, 2L)).thenReturn(Optional.of(campo));
        when(reservaRepository.cantidadTomadaPorCliente(100L, 1L)).thenReturn(3L);

        assertThatThrownBy(() -> service.crear(100L, new ReservaRequest(1L, 2L, null, 2)))
                .isInstanceOf(LimiteExcedidoException.class)
                .hasMessageContaining("máximo de 4");
        verify(sectorRepository, never()).descontar(any(), any(), anyInt());
    }

    @Test
    void unSectorNumeradoExigeElegirAsientos() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(sectorRepository.findByEventoIdAndSectorId(1L, 1L)).thenReturn(Optional.of(platea));

        assertThatThrownBy(() -> service.crear(100L, new ReservaRequest(1L, 1L, null, 2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noSePuedenElegirAsientosEnUnSectorNoNumerado() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(sectorRepository.findByEventoIdAndSectorId(1L, 2L)).thenReturn(Optional.of(campo));

        assertThatThrownBy(() -> service.crear(100L, new ReservaRequest(1L, 2L, List.of(1L), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noSePuedeReservarSiLaVentaEstaDeshabilitada() {
        evento.setHabilitado(false);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> service.crear(100L, new ReservaRequest(1L, 2L, null, 1)))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void confirmarUnaReservaYaConfirmadaEsIdempotente() {
        Reserva r = new Reserva(100L, 1L, campo, 1, AHORA, 10);
        r.setId(7L);
        r.setEstado(EstadoReserva.CONFIRMADA);
        when(reservaRepository.findById(7L)).thenReturn(Optional.of(r));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

        var resp = service.confirmar(7L);

        assertThat(resp.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        verify(reservaRepository, never()).confirmarSiVigente(any(), any());
    }

    @Test
    void noSePuedeConfirmarUnaReservaVencida() {
        Reserva r = new Reserva(100L, 1L, campo, 1, AHORA.minusMinutes(15), 10);
        r.setId(7L);
        when(reservaRepository.findById(7L)).thenReturn(Optional.of(r));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(reservaRepository.confirmarSiVigente(eq(7L), any())).thenReturn(0);

        assertThatThrownBy(() -> service.confirmar(7L))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("VENCIDA");
    }

    @Test
    void liberarUnaReservaConfirmadaFalla() {
        Reserva r = new Reserva(100L, 1L, campo, 1, AHORA, 10);
        r.setId(7L);
        r.setEstado(EstadoReserva.CONFIRMADA);
        when(reservaRepository.findById(7L)).thenReturn(Optional.of(r));
        when(reservaRepository.finalizarSiPendiente(eq(7L), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.liberar(7L, EstadoReserva.LIBERADA))
                .isInstanceOf(OperacionInvalidaException.class);
        verify(sectorRepository, never()).reponer(any(), any(), anyInt());
    }

    @Test
    void liberarDosVecesSoloReponeElCupoUnaVez() {
        Reserva r = new Reserva(100L, 1L, campo, 2, AHORA, 10);
        r.setId(7L);
        when(reservaRepository.findById(7L)).thenReturn(Optional.of(r));
        when(reservaRepository.finalizarSiPendiente(eq(7L), any(), any())).thenReturn(1, 0);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));

        service.liberar(7L, EstadoReserva.LIBERADA);
        r.setEstado(EstadoReserva.LIBERADA);
        service.liberar(7L, EstadoReserva.LIBERADA);

        verify(sectorRepository, times(1)).reponer(1L, 2L, 2);
    }
}
