package com.ticketya.compras.service;

import com.ticketya.compras.client.PagoDto;
import com.ticketya.compras.client.ReservaDto;
import com.ticketya.compras.dto.CompraRequest;
import com.ticketya.compras.exception.*;
import com.ticketya.compras.model.EstadoCompra;
import com.ticketya.compras.model.EstadoEntrada;
import com.ticketya.compras.repository.EntradaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class CompraServiceTest extends ComprasTestBase {

    @Autowired
    private EntradaRepository entradaRepository;

    private final LocalDateTime enUnMes = LocalDateTime.now().plusDays(30);

    @Test
    void pagoAprobadoConfirmaLaReservaYEmiteUnaEntradaPorLugarConQrUnico() {
        ReservaDto reserva = prepararCompraAprobada(nuevoId(), enUnMes);

        ResultadoCompra r = comprar(reserva.id());

        assertThat(r.status()).isEqualTo(HttpStatus.CREATED);
        assertThat(r.compra().estado()).isEqualTo(EstadoCompra.APROBADA);
        assertThat(r.compra().montoTotal()).isEqualByComparingTo("90000");
        assertThat(r.compra().entradas()).hasSize(2)
                .allSatisfy(e -> {
                    assertThat(e.estado()).isEqualTo(EstadoEntrada.VALIDA);
                    assertThat(e.codigoQr()).hasSize(36);
                    assertThat(e.precio()).isEqualByComparingTo("45000");
                });
        assertThat(r.compra().entradas().get(0).codigoQr()).isNotEqualTo(r.compra().entradas().get(1).codigoQr());
        verify(reservasClient).confirmar(reserva.id());
        verify(pagosClient).pagar(eq(r.compra().id()), eq(CLIENTE), any(), any(), eq("compra-" + r.compra().id()));
    }

    @Test
    void pagoRechazadoLiberaLaReservaYNoEmiteEntradas() {
        long reservaId = nuevoId();
        when(reservasClient.obtener(reservaId)).thenReturn(reservaPendiente(reservaId, nuevoId(), enUnMes));
        when(pagosClient.pagar(any(), any(), any(), any(), anyString()))
                .thenReturn(new PagoDto(5L, 1L, BigDecimal.TEN, "RECHAZADO", null, "Fondos insuficientes"));

        ResultadoCompra r = compraService.comprar(CLIENTE, new CompraRequest(reservaId, TARJETA_SIN_FONDOS), "k-" + reservaId);

        assertThat(r.status()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        assertThat(r.compra().estado()).isEqualTo(EstadoCompra.RECHAZADA);
        assertThat(r.compra().detalle()).contains("Fondos insuficientes");
        assertThat(r.compra().entradas()).isEmpty();
        verify(reservasClient).liberar(reservaId);
        verify(reservasClient, never()).confirmar(any());
    }

    @Test
    void reintentarConLaMismaClaveDevuelveLoMismoSinCobrarDeNuevo() {
        ReservaDto reserva = prepararCompraAprobada(nuevoId(), enUnMes);
        String clave = UUID.randomUUID().toString();
        CompraRequest req = new CompraRequest(reserva.id(), TARJETA_OK);

        ResultadoCompra primero = compraService.comprar(CLIENTE, req, clave);
        ResultadoCompra segundo = compraService.comprar(CLIENTE, req, clave);

        assertThat(segundo.status()).isEqualTo(HttpStatus.OK);
        assertThat(segundo.compra().id()).isEqualTo(primero.compra().id());
        assertThat(segundo.compra().entradas()).hasSize(2);
        verify(pagosClient, times(1)).pagar(any(), any(), any(), any(), anyString());
    }

    @Test
    void laMismaClaveNoSePuedeUsarParaOtraReserva() {
        ReservaDto reserva = prepararCompraAprobada(nuevoId(), enUnMes);
        String clave = UUID.randomUUID().toString();
        compraService.comprar(CLIENTE, new CompraRequest(reserva.id(), TARJETA_OK), clave);

        assertThatThrownBy(() -> compraService.comprar(CLIENTE, new CompraRequest(999_999L, TARJETA_OK), clave))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void noSePuedeComprarDosVecesLaMismaReservaConClavesDistintas() {
        ReservaDto reserva = prepararCompraAprobada(nuevoId(), enUnMes);
        comprar(reserva.id());

        assertThatThrownBy(() -> comprar(reserva.id()))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("Ya existe la compra");
    }

    @Test
    void siLaPasarelaNoRespondeLaCompraQuedaEnErrorYSePuedeReintentar() {
        ReservaDto reserva = prepararCompraAprobada(nuevoId(), enUnMes);
        String clave = UUID.randomUUID().toString();
        CompraRequest req = new CompraRequest(reserva.id(), TARJETA_OK);
        when(pagosClient.pagar(any(), any(), any(), any(), anyString()))
                .thenThrow(new ServicioNoDisponibleException("ms-pagos no está disponible", null))
                .thenAnswer(inv -> new PagoDto(77L, inv.getArgument(0), inv.getArgument(2), "APROBADO", "AUT", null));

        ResultadoCompra conError = compraService.comprar(CLIENTE, req, clave);
        assertThat(conError.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(conError.compra().estado()).isEqualTo(EstadoCompra.ERROR);

        ResultadoCompra reintento = compraService.comprar(CLIENTE, req, clave);
        assertThat(reintento.status()).isEqualTo(HttpStatus.CREATED);
        assertThat(reintento.compra().id()).isEqualTo(conError.compra().id());
        assertThat(reintento.compra().estado()).isEqualTo(EstadoCompra.APROBADA);
        // las dos llamadas a ms-pagos usan la misma clave, asi la pasarela no cobra dos veces
        verify(pagosClient, times(2)).pagar(any(), any(), any(), any(), eq("compra-" + conError.compra().id()));
    }

    @Test
    void siLaReservaVencioMientrasSePagabaSeReintegraElDinero() {
        ReservaDto reserva = prepararCompraAprobada(nuevoId(), enUnMes);
        when(reservasClient.confirmar(reserva.id()))
                .thenThrow(new RespuestaServicioException(409, "La reserva no se puede confirmar: está VENCIDA"));

        ResultadoCompra r = comprar(reserva.id());

        assertThat(r.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.compra().estado()).isEqualTo(EstadoCompra.RECHAZADA);
        assertThat(r.compra().entradas()).isEmpty();
        verify(pagosClient).reintegrar(eq(r.compra().pagoId()), argThat(m -> m.compareTo(new BigDecimal("90000")) == 0),
                anyString(), eq("compensacion-compra-" + r.compra().id()));
    }

    @Test
    void noSePuedePagarLaReservaDeOtroCliente() {
        long reservaId = nuevoId();
        when(reservasClient.obtener(reservaId)).thenReturn(reservaPendiente(reservaId, nuevoId(), enUnMes));

        assertThatThrownBy(() -> compraService.comprar(999L, new CompraRequest(reservaId, TARJETA_OK), "x-" + reservaId))
                .isInstanceOf(AccesoDenegadoException.class);
        verifyNoInteractions(pagosClient);
    }

    @Test
    void noSePuedePagarUnaReservaVencida() {
        long reservaId = nuevoId();
        ReservaDto r = reservaPendiente(reservaId, nuevoId(), enUnMes);
        ReservaDto vencida = new ReservaDto(r.id(), r.usuarioId(), r.eventoId(), r.nombreEvento(), r.fechaEvento(),
                r.sectorId(), r.sector(), r.cantidad(), r.precioUnitario(), r.montoTotal(), "VENCIDA", r.expiraEn(), 0,
                r.items());
        when(reservasClient.obtener(reservaId)).thenReturn(vencida);

        assertThatThrownBy(() -> comprar(reservaId)).isInstanceOf(OperacionInvalidaException.class);
        verifyNoInteractions(pagosClient);
    }

    @Test
    void laBaseRechazaDosEntradasValidasParaElMismoAsiento() {
        // dos reservas distintas que (por un error en otro servicio) traen el mismo asiento
        long eventoId = nuevoId();
        ReservaDto a = prepararCompraAprobada(eventoId, enUnMes);
        ReservaDto b = prepararCompraAprobada(eventoId, enUnMes);
        comprar(a.id());

        assertThatThrownBy(() -> comprar(b.id())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(entradaRepository.findByEventoIdAndEstado(eventoId, EstadoEntrada.VALIDA)).hasSize(2);
    }
}
