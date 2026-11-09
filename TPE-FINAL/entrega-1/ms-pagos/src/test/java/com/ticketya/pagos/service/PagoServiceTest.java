package com.ticketya.pagos.service;

import com.ticketya.pagos.dto.DatosMedioPago;
import com.ticketya.pagos.dto.PagoRequest;
import com.ticketya.pagos.dto.ReintegroRequest;
import com.ticketya.pagos.exception.ClaveIdempotenciaReutilizadaException;
import com.ticketya.pagos.exception.OperacionInvalidaException;
import com.ticketya.pagos.model.EstadoPago;
import com.ticketya.pagos.model.MedioPago;
import com.ticketya.pagos.repository.PagoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PagoServiceTest {

    @Autowired
    private PagoService pagoService;
    @Autowired
    private PagoRepository pagoRepository;

    private PagoRequest pedido(long compraId, String tarjeta, String monto) {
        return new PagoRequest(compraId, 100L, new BigDecimal(monto),
                new DatosMedioPago(MedioPago.TARJETA_CREDITO, tarjeta, "Juan Perez", 1));
    }

    private String clave() {
        return "test-" + UUID.randomUUID();
    }

    @Test
    void unaTarjetaComunSeAprueba() {
        var r = pagoService.pagar(pedido(1, "4509953566233704", "90000"), clave());

        assertThat(r.creado()).isTrue();
        assertThat(r.cuerpo().estado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(r.cuerpo().codigoAutorizacion()).startsWith("AUT-");
        assertThat(r.cuerpo().ultimosDigitos()).isEqualTo("3704");
    }

    @Test
    void unaTarjetaTerminadaEn0002SeRechazaPorFondos() {
        var r = pagoService.pagar(pedido(2, "4000000000000002", "90000"), clave());

        assertThat(r.cuerpo().estado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(r.cuerpo().motivoRechazo()).isEqualTo("Fondos insuficientes");
    }

    @Test
    void reintentarConLaMismaClaveNoCobraDosVeces() {
        String clave = clave();
        var primero = pagoService.pagar(pedido(3, "4509953566233704", "50000"), clave);
        var segundo = pagoService.pagar(pedido(3, "4509953566233704", "50000"), clave);

        assertThat(segundo.creado()).isFalse();
        assertThat(segundo.cuerpo().id()).isEqualTo(primero.cuerpo().id());
        assertThat(pagoRepository.findByCompraIdOrderByIdDesc(3L)).hasSize(1);
    }

    @Test
    void reutilizarLaClaveParaOtraCompraEsUnError() {
        String clave = clave();
        pagoService.pagar(pedido(4, "4509953566233704", "50000"), clave);

        assertThatThrownBy(() -> pagoService.pagar(pedido(5, "4509953566233704", "50000"), clave))
                .isInstanceOf(ClaveIdempotenciaReutilizadaException.class);
    }

    @Test
    void losReintegrosNoPuedenSuperarLoCobrado() {
        var pago = pagoService.pagar(pedido(6, "4509953566233704", "100000"), clave()).cuerpo();

        pagoService.reintegrar(pago.id(), new ReintegroRequest(new BigDecimal("60000"), "parcial"), clave());

        assertThatThrownBy(() -> pagoService.reintegrar(pago.id(),
                new ReintegroRequest(new BigDecimal("50000"), "otro"), clave()))
                .isInstanceOf(OperacionInvalidaException.class);
        assertThat(pagoService.obtener(pago.id()).totalReintegrado()).isEqualByComparingTo("60000");
    }

    @Test
    void elReintegroEsIdempotente() {
        var pago = pagoService.pagar(pedido(7, "4509953566233704", "100000"), clave()).cuerpo();
        String clave = clave();

        pagoService.reintegrar(pago.id(), new ReintegroRequest(new BigDecimal("100000"), "total"), clave);
        var repetido = pagoService.reintegrar(pago.id(), new ReintegroRequest(new BigDecimal("100000"), "total"), clave);

        assertThat(repetido.creado()).isFalse();
        assertThat(pagoService.obtener(pago.id()).reintegros()).hasSize(1);
    }

    @Test
    void noSePuedeReintegrarUnPagoRechazado() {
        var pago = pagoService.pagar(pedido(8, "4000000000000069", "100000"), clave()).cuerpo();

        assertThatThrownBy(() -> pagoService.reintegrar(pago.id(),
                new ReintegroRequest(BigDecimal.TEN, "x"), clave()))
                .isInstanceOf(OperacionInvalidaException.class);
    }
}
