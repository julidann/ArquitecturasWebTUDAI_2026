package com.ticketya.compras.service;

import com.ticketya.compras.client.PagoDto;
import com.ticketya.compras.client.ReservaDto;
import com.ticketya.compras.exception.RespuestaServicioException;
import com.ticketya.compras.exception.ServicioNoDisponibleException;
import com.ticketya.compras.mensajeria.MensajeSalienteRepository;
import com.ticketya.compras.model.EstadoCompra;
import com.ticketya.compras.repository.CompraRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Fallos parciales de la saga de compra: la compra queda en ERROR y el job de recuperacion la resuelve
 * consultando a la pasarela que paso con el cobro.
 */
@SpringBootTest
class RecuperacionComprasTest extends ComprasTestBase {

    @Autowired
    RecuperacionComprasJob job;
    @Autowired
    CompraRepository compraRepository;
    @Autowired
    MensajeSalienteRepository outbox;

    private final LocalDateTime enUnMes = LocalDateTime.now().plusDays(30);

    /** Compra que quedo en ERROR porque la pasarela no respondio al cobrar. */
    private Long compraEnError(ReservaDto reserva) {
        when(reservasClient.obtener(reserva.id())).thenReturn(reserva);
        when(pagosClient.pagar(any(), any(), any(), any(), anyString()))
                .thenThrow(new ServicioNoDisponibleException("timeout", null));
        ResultadoCompra r = comprar(reserva.id());
        assertThat(r.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(r.compra().estado()).isEqualTo(EstadoCompra.ERROR);
        return r.compra().id();
    }

    @Test
    void siLaPasarelaHabiaCobradoSeCompletaLaCompraYSeEmitenLasEntradas() {
        ReservaDto reserva = reservaPendiente(nuevoId(), nuevoId(), enUnMes);
        Long id = compraEnError(reserva);
        when(pagosClient.buscarPorCompra(id)).thenReturn(List.of(
                new PagoDto(900L, id, new BigDecimal("90000"), "APROBADO", "AUT", null)));
        when(reservasClient.confirmar(reserva.id())).thenReturn(confirmada(reserva));
        long mensajesAntes = outbox.count();

        assertThat(job.recuperar(id)).isTrue();

        var compra = compraService.obtener(id, CLIENTE);
        assertThat(compra.estado()).isEqualTo(EstadoCompra.APROBADA);
        assertThat(compra.entradas()).hasSize(2);
        assertThat(outbox.count()).isEqualTo(mensajesAntes + 1); // compra.confirmada
    }

    @Test
    void siLaPasarelaNoRegistraNingunCobroSeCierraSinCobrarYSeLiberaLaReserva() {
        ReservaDto reserva = reservaPendiente(nuevoId(), nuevoId(), enUnMes);
        Long id = compraEnError(reserva);
        when(pagosClient.buscarPorCompra(id)).thenReturn(List.of());

        assertThat(job.recuperar(id)).isTrue();

        assertThat(compraService.obtener(id, CLIENTE).estado()).isEqualTo(EstadoCompra.RECHAZADA);
        verify(reservasClient).liberar(reserva.id());
        verify(pagosClient, never()).reintegrar(any(), any(), any(), any());
    }

    @Test
    void siElPagoSeAprobaPeroLaReservaYaVencioSeCompensaConUnReintegro() {
        ReservaDto reserva = reservaPendiente(nuevoId(), nuevoId(), enUnMes);
        Long id = compraEnError(reserva);
        when(pagosClient.buscarPorCompra(id)).thenReturn(List.of(
                new PagoDto(901L, id, new BigDecimal("90000"), "APROBADO", "AUT", null)));
        when(reservasClient.confirmar(reserva.id())).thenThrow(new RespuestaServicioException(409, "VENCIDA"));

        assertThat(job.recuperar(id)).isTrue();

        assertThat(compraService.obtener(id, CLIENTE).estado()).isEqualTo(EstadoCompra.RECHAZADA);
        verify(pagosClient).reintegrar(eq(901L), any(), any(), eq("compensacion-compra-" + id));
    }

    @Test
    void siLaPasarelaSigueCaidaLaCompraQuedaEnErrorParaElProximoIntento() {
        ReservaDto reserva = reservaPendiente(nuevoId(), nuevoId(), enUnMes);
        Long id = compraEnError(reserva);
        when(pagosClient.buscarPorCompra(id)).thenThrow(new ServicioNoDisponibleException("sigue caida", null));

        assertThat(job.recuperar(id)).isFalse();

        assertThat(compraRepository.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoCompra.ERROR);
        assertThat(compraRepository.idsEnErrorAntesDe(LocalDateTime.now().plusSeconds(1))).contains(id);
    }

    @Test
    void unaCompraSoloLaReclamaUnoAunqueLoIntentenVarios() {
        ReservaDto reserva = reservaPendiente(nuevoId(), nuevoId(), enUnMes);
        Long id = compraEnError(reserva);

        assertThat(compraService.reclamar(id)).isTrue();
        assertThat(compraService.reclamar(id)).isFalse();
        assertThat(job.recuperar(id)).isFalse(); // ya esta tomada (PENDIENTE)
    }
}
