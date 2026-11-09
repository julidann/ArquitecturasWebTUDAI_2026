package com.ticketya.eventos.service;

import com.ticketya.eventos.client.ComprasClient;
import com.ticketya.eventos.client.DisponibilidadInicialRequest;
import com.ticketya.eventos.client.ReservasClient;
import com.ticketya.eventos.dto.EventoRequest;
import com.ticketya.eventos.dto.EventoResponse;
import com.ticketya.eventos.dto.PrecioSectorRequest;
import com.ticketya.eventos.dto.PreciosRequest;
import com.ticketya.eventos.exception.AccesoDenegadoException;
import com.ticketya.eventos.exception.OperacionInvalidaException;
import com.ticketya.eventos.exception.ServicioExternoException;
import com.ticketya.eventos.model.*;
import com.ticketya.eventos.repository.EventoRepository;
import com.ticketya.eventos.repository.LugarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    private static final Long ORGANIZADOR = 10L;

    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private LugarRepository lugarRepository;
    @Mock
    private ReservasClient reservasClient;
    @Mock
    private ComprasClient comprasClient;

    @InjectMocks
    private EventoService eventoService;

    private Lugar lugar;
    private Sector platea;
    private Sector campo;

    @BeforeEach
    void setUp() {
        lugar = new Lugar("Teatro del Lago", "Av. Colón 1234", "Tandil", -37.32, -59.13);
        lugar.setId(1L);
        platea = Sector.numerado("Platea", 2, 3);
        platea.setId(1L);
        long idAsiento = 1;
        for (Asiento a : platea.getAsientos()) {
            a.setId(idAsiento++);
        }
        campo = Sector.noNumerado("Campo", 100);
        campo.setId(2L);
        lugar.agregarSector(platea);
        lugar.agregarSector(campo);
    }

    private Evento eventoEnBorrador() {
        Evento e = new Evento("Recital de prueba", "desc", TipoEvento.RECITAL, ORGANIZADOR,
                List.of("Banda X"), LocalDateTime.now().plusDays(20), lugar);
        e.setId(5L);
        return e;
    }

    @Test
    void crearEventoLoDejaEnBorradorConElOrganizadorDelHeader() {
        when(lugarRepository.findById(1L)).thenReturn(Optional.of(lugar));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(inv -> inv.getArgument(0));
        EventoRequest req = new EventoRequest("Recital", "d", TipoEvento.RECITAL, List.of("Banda X"),
                LocalDateTime.now().plusDays(10), 1L, false, 4);

        EventoResponse resp = eventoService.crear(req, ORGANIZADOR, Rol.ORGANIZADOR);

        assertThat(resp.estado()).isEqualTo(EstadoEvento.BORRADOR);
        assertThat(resp.organizadorId()).isEqualTo(ORGANIZADOR);
        assertThat(resp.limiteEntradasPorCliente()).isEqualTo(4);
    }

    @Test
    void unClienteNoPuedeCrearEventos() {
        EventoRequest req = new EventoRequest("Recital", "d", TipoEvento.RECITAL, List.of(),
                LocalDateTime.now().plusDays(10), 1L, false, null);

        assertThatThrownBy(() -> eventoService.crear(req, 99L, Rol.CLIENTE))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void definirPreciosCalculaElPrecioMinimo() {
        Evento evento = eventoEnBorrador();
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));

        EventoResponse resp = eventoService.definirPrecios(5L, new PreciosRequest(List.of(
                new PrecioSectorRequest(1L, new BigDecimal("50000")),
                new PrecioSectorRequest(2L, new BigDecimal("30000")))), ORGANIZADOR, Rol.ORGANIZADOR);

        assertThat(resp.precioMinimo()).isEqualByComparingTo("30000");
        assertThat(resp.precios()).hasSize(2);
    }

    @Test
    void noSePuedePonerPrecioAUnSectorDeOtroLugar() {
        Evento evento = eventoEnBorrador();
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> eventoService.definirPrecios(5L, new PreciosRequest(List.of(
                new PrecioSectorRequest(999L, BigDecimal.TEN))), ORGANIZADOR, Rol.ORGANIZADOR))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("no pertenece");
    }

    @Test
    void otroOrganizadorNoPuedeModificarElEvento() {
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(eventoEnBorrador()));

        assertThatThrownBy(() -> eventoService.publicar(5L, 77L, Rol.ORGANIZADOR))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void noSePuedePublicarSinPrecioEnTodosLosSectores() {
        Evento evento = eventoEnBorrador();
        evento.definirPrecio(platea, new BigDecimal("50000"));
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> eventoService.publicar(5L, ORGANIZADOR, Rol.ORGANIZADOR))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("precio");
        verifyNoInteractions(reservasClient);
        assertThat(evento.getEstado()).isEqualTo(EstadoEvento.BORRADOR);
    }

    @Test
    void publicarInicializaLaDisponibilidadEnReservasConAsientosYCupos() {
        Evento evento = eventoEnBorrador();
        evento.definirPrecio(platea, new BigDecimal("50000"));
        evento.definirPrecio(campo, new BigDecimal("30000"));
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));

        EventoResponse resp = eventoService.publicar(5L, ORGANIZADOR, Rol.ORGANIZADOR);

        assertThat(resp.estado()).isEqualTo(EstadoEvento.PUBLICADO);
        ArgumentCaptor<DisponibilidadInicialRequest> captor = ArgumentCaptor.forClass(DisponibilidadInicialRequest.class);
        verify(reservasClient).inicializarDisponibilidad(captor.capture());
        DisponibilidadInicialRequest enviado = captor.getValue();
        assertThat(enviado.eventoId()).isEqualTo(5L);
        assertThat(enviado.sectores()).hasSize(2);
        assertThat(enviado.sectores()).filteredOn(s -> s.tipo() == TipoSector.NUMERADO)
                .singleElement()
                .satisfies(s -> assertThat(s.asientos()).hasSize(6));
        assertThat(enviado.sectores()).filteredOn(s -> s.tipo() == TipoSector.NO_NUMERADO)
                .singleElement()
                .satisfies(s -> assertThat(s.capacidad()).isEqualTo(100));
    }

    @Test
    void siReservasFallaElEventoNoSePublica() {
        Evento evento = eventoEnBorrador();
        evento.definirPrecio(platea, BigDecimal.TEN);
        evento.definirPrecio(campo, BigDecimal.ONE);
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));
        doThrow(new ServicioExternoException("caido", null)).when(reservasClient).inicializarDisponibilidad(any());

        assertThatThrownBy(() -> eventoService.publicar(5L, ORGANIZADOR, Rol.ORGANIZADOR))
                .isInstanceOf(ServicioExternoException.class);
        assertThat(evento.getEstado()).isEqualTo(EstadoEvento.BORRADOR);
    }

    @Test
    void soloElAdminPuedeCancelar() {
        assertThatThrownBy(() -> eventoService.cancelar(5L, "motivo", Rol.ORGANIZADOR))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void cancelarUnEventoPublicadoDeshabilitaVentasYPideReintegros() {
        Evento evento = eventoEnBorrador();
        evento.cambiarEstado(EstadoEvento.PUBLICADO);
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));

        EventoResponse resp = eventoService.cancelar(5L, "Lluvia", Rol.ADMIN);

        assertThat(resp.estado()).isEqualTo(EstadoEvento.CANCELADO);
        verify(reservasClient).cambiarHabilitacion(5L, false, "Lluvia");
        verify(comprasClient).notificarCancelacion(5L, "Lluvia");
    }

    @Test
    void cancelarUnBorradorNoLlamaAOtrosServicios() {
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(eventoEnBorrador()));

        eventoService.cancelar(5L, null, Rol.ADMIN);

        verifyNoInteractions(reservasClient, comprasClient);
    }

    @Test
    void noSePuedeCancelarUnEventoFinalizado() {
        Evento evento = eventoEnBorrador();
        evento.cambiarEstado(EstadoEvento.FINALIZADO);
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> eventoService.cancelar(5L, null, Rol.ADMIN))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void suspenderSoloAplicaAEventosPublicados() {
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(eventoEnBorrador()));

        assertThatThrownBy(() -> eventoService.suspender(5L, "x", Rol.ADMIN))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void marcarAgotadoSoloCambiaEventosPublicados() {
        Evento evento = eventoEnBorrador();
        evento.cambiarEstado(EstadoEvento.CANCELADO);
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));

        eventoService.marcarAgotado(5L);

        assertThat(evento.getEstado()).isEqualTo(EstadoEvento.CANCELADO);
    }
}
