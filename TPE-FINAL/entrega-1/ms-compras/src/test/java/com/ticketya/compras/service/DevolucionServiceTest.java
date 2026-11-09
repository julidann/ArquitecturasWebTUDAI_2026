package com.ticketya.compras.service;

import com.ticketya.compras.client.ReintegroDto;
import com.ticketya.compras.client.ReservaDto;
import com.ticketya.compras.dto.EntradaResponse;
import com.ticketya.compras.dto.SolicitudDevolucionResponse;
import com.ticketya.compras.exception.*;
import com.ticketya.compras.model.EstadoEntrada;
import com.ticketya.compras.model.EstadoSolicitud;
import com.ticketya.compras.model.Rol;
import com.ticketya.compras.repository.EntradaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class DevolucionServiceTest extends ComprasTestBase {

    private static final Long ADMIN = 1L;

    @Autowired
    private DevolucionService devolucionService;
    @Autowired
    private CancelacionEventoService cancelacionService;
    @Autowired
    private EntradaRepository entradaRepository;

    private EntradaResponse comprarEntrada(LocalDateTime fechaEvento) {
        ReservaDto reserva = prepararCompraAprobada(nuevoId(), fechaEvento);
        return comprar(reserva.id()).compra().entradas().get(0);
    }

    @Test
    void sePuedeSolicitarConMasDe72HorasDeAnticipacion() {
        EntradaResponse entrada = comprarEntrada(LocalDateTime.now().plusHours(73));

        SolicitudDevolucionResponse sol = devolucionService.solicitar(entrada.id(), CLIENTE, "No puedo ir");

        assertThat(sol.estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(sol.montoAReintegrar()).isEqualByComparingTo("45000");
    }

    @Test
    void noSePuedeSolicitarConMenosDe72Horas() {
        EntradaResponse entrada = comprarEntrada(LocalDateTime.now().plusHours(71));

        assertThatThrownBy(() -> devolucionService.solicitar(entrada.id(), CLIENTE, "tarde"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("72 horas");
    }

    @Test
    void noSePuedeDevolverLaEntradaDeOtro() {
        EntradaResponse entrada = comprarEntrada(LocalDateTime.now().plusDays(10));

        assertThatThrownBy(() -> devolucionService.solicitar(entrada.id(), 555L, "x"))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void noSePuedenTenerDosSolicitudesPendientesParaLaMismaEntrada() {
        EntradaResponse entrada = comprarEntrada(LocalDateTime.now().plusDays(10));
        devolucionService.solicitar(entrada.id(), CLIENTE, "x");

        assertThatThrownBy(() -> devolucionService.solicitar(entrada.id(), CLIENTE, "otra vez"))
                .isInstanceOf(OperacionInvalidaException.class);
    }

    @Test
    void aprobarLiberaElLugarReintegraYMarcaLaEntradaDevuelta() {
        EntradaResponse entrada = comprarEntrada(LocalDateTime.now().plusDays(10));
        SolicitudDevolucionResponse sol = devolucionService.solicitar(entrada.id(), CLIENTE, "x");
        when(pagosClient.reintegrar(any(), any(), any(), anyString()))
                .thenReturn(new ReintegroDto(9L, 1L, new BigDecimal("45000"), "APROBADO"));

        SolicitudDevolucionResponse aprobada = devolucionService.aprobar(sol.id(), ADMIN, Rol.ADMIN);

        assertThat(aprobada.estado()).isEqualTo(EstadoSolicitud.APROBADA);
        assertThat(aprobada.reintegroId()).isEqualTo(9L);
        String clave = "devolucion-" + sol.id();
        verify(reservasClient).liberarVendido(eq(entrada.eventoId()), any(), eq(entrada.sectorId()),
                eq(entrada.asientoId()), eq(clave));
        verify(pagosClient).reintegrar(any(), argThat(m -> m.compareTo(new BigDecimal("45000")) == 0), anyString(), eq(clave));
        var guardada = entradaRepository.findById(entrada.id()).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo(EstadoEntrada.DEVUELTA);
        assertThat(guardada.getLugarActivo()).as("libera la restriccion de unicidad del asiento").isNull();
    }

    @Test
    void soloUnAdminPuedeAprobar() {
        EntradaResponse entrada = comprarEntrada(LocalDateTime.now().plusDays(10));
        SolicitudDevolucionResponse sol = devolucionService.solicitar(entrada.id(), CLIENTE, "x");

        assertThatThrownBy(() -> devolucionService.aprobar(sol.id(), CLIENTE, Rol.CLIENTE))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void siFallaElReintegroLaSolicitudSiguePendienteYSePuedeReintentar() {
        EntradaResponse entrada = comprarEntrada(LocalDateTime.now().plusDays(10));
        SolicitudDevolucionResponse sol = devolucionService.solicitar(entrada.id(), CLIENTE, "x");
        when(pagosClient.reintegrar(any(), any(), any(), anyString()))
                .thenThrow(new ServicioNoDisponibleException("ms-pagos no está disponible", null))
                .thenReturn(new ReintegroDto(9L, 1L, new BigDecimal("45000"), "APROBADO"));

        assertThatThrownBy(() -> devolucionService.aprobar(sol.id(), ADMIN, Rol.ADMIN))
                .isInstanceOf(ServicioNoDisponibleException.class);
        assertThat(entradaRepository.findById(entrada.id()).orElseThrow().getEstado()).isEqualTo(EstadoEntrada.VALIDA);

        SolicitudDevolucionResponse reintento = devolucionService.aprobar(sol.id(), ADMIN, Rol.ADMIN);
        assertThat(reintento.estado()).isEqualTo(EstadoSolicitud.APROBADA);
        // las dos veces se libero con la misma clave: ms-reservas no libera el lugar dos veces
        verify(reservasClient, times(2)).liberarVendido(any(), any(), any(), any(), eq("devolucion-" + sol.id()));
    }

    @Test
    void unaSolicitudResueltaNoSePuedeVolverAResolver() {
        EntradaResponse entrada = comprarEntrada(LocalDateTime.now().plusDays(10));
        SolicitudDevolucionResponse sol = devolucionService.solicitar(entrada.id(), CLIENTE, "x");
        devolucionService.rechazar(sol.id(), ADMIN, Rol.ADMIN, "No corresponde");

        assertThatThrownBy(() -> devolucionService.aprobar(sol.id(), ADMIN, Rol.ADMIN))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThat(entradaRepository.findById(entrada.id()).orElseThrow().getEstado()).isEqualTo(EstadoEntrada.VALIDA);
    }

    @Test
    void cancelarUnEventoReintegraTodasLasEntradasValidasUnaSolaVez() {
        long eventoId = nuevoId();
        ReservaDto r1 = prepararCompraAprobadaEnEvento(eventoId, 1);
        ReservaDto r2 = prepararCompraAprobadaEnEvento(eventoId, 2);
        comprar(r1.id());
        comprar(r2.id());
        when(pagosClient.reintegrar(any(), any(), any(), anyString()))
                .thenReturn(new ReintegroDto(1L, 1L, BigDecimal.ONE, "APROBADO"));

        var resultado = cancelacionService.cancelarEvento(eventoId, "Suspendido por lluvia");
        var repetido = cancelacionService.cancelarEvento(eventoId, "Suspendido por lluvia");

        assertThat(resultado.entradasCanceladas()).isEqualTo(4);
        assertThat(resultado.compradoresNotificados()).isEqualTo(1);
        assertThat(repetido.entradasCanceladas()).as("la segunda llamada no reprocesa nada").isZero();
        verify(pagosClient, times(4)).reintegrar(any(), any(), any(), startsWith("cancelacion-entrada-"));
        assertThat(entradaRepository.findByEventoIdAndEstado(eventoId, EstadoEntrada.CANCELADA)).hasSize(4);
    }

    /** Igual que prepararCompraAprobada pero con asientos distintos para poder comprar varias veces el mismo evento. */
    private ReservaDto prepararCompraAprobadaEnEvento(long eventoId, int fila) {
        long reservaId = nuevoId();
        ReservaDto base = reservaPendiente(reservaId, eventoId, LocalDateTime.now().plusDays(10));
        ReservaDto reserva = new ReservaDto(base.id(), base.usuarioId(), eventoId, base.nombreEvento(),
                base.fechaEvento(), base.sectorId(), base.sector(), 2, base.precioUnitario(), base.montoTotal(),
                "PENDIENTE", base.expiraEn(), 480, java.util.List.of(
                new ReservaDto.Item(eventoId * 1000 + fila * 10 + 1, "F" + fila, 1),
                new ReservaDto.Item(eventoId * 1000 + fila * 10 + 2, "F" + fila, 2)));
        when(reservasClient.obtener(reservaId)).thenReturn(reserva);
        when(reservasClient.confirmar(reservaId)).thenReturn(confirmada(reserva));
        when(pagosClient.pagar(any(), eq(CLIENTE), any(), any(), anyString()))
                .thenAnswer(inv -> new com.ticketya.compras.client.PagoDto(1000L + (Long) inv.getArgument(0),
                        inv.getArgument(0), inv.getArgument(2), "APROBADO", "AUT-TEST", null));
        return reserva;
    }
}
