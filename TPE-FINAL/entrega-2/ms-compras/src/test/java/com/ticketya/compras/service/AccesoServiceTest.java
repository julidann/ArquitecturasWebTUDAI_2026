package com.ticketya.compras.service;

import com.ticketya.compras.client.ReservaDto;
import com.ticketya.compras.dto.EntradaResponse;
import com.ticketya.compras.dto.ValidacionAccesoRequest;
import com.ticketya.compras.dto.ValidacionAccesoResponse;
import com.ticketya.compras.model.EstadoEntrada;
import com.ticketya.compras.repository.EntradaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/** Control de acceso con QR (funcionalidad 8) con base H2 real. */
@SpringBootTest
class AccesoServiceTest extends ComprasTestBase {

    @Autowired
    AccesoService accesoService;
    @Autowired
    EntradaRepository entradaRepository;

    private EntradaResponse entradaComprada(long eventoId) {
        ReservaDto reserva = prepararCompraAprobada(eventoId, LocalDateTime.now().plusDays(10));
        return comprar(reserva.id()).compra().entradas().get(0);
    }

    @Test
    void unQrValidoDejaPasarYMarcaLaEntradaComoUtilizada() {
        long evento = nuevoId();
        EntradaResponse e = entradaComprada(evento);

        ValidacionAccesoResponse r = accesoService.validar(new ValidacionAccesoRequest(e.codigoQr(), evento, "Puerta 1"), 6L);

        assertThat(r.permitido()).isTrue();
        assertThat(r.resultado()).isEqualTo("PERMITIDO");
        assertThat(r.fila()).isEqualTo("C");
        var guardada = entradaRepository.findById(e.id()).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo(EstadoEntrada.UTILIZADA);
        assertThat(guardada.getUtilizadaEn()).isNotNull();
        assertThat(guardada.getPuertaIngreso()).isEqualTo("Puerta 1");
    }

    @Test
    void unQrNoPuedeIngresarDosVeces() {
        long evento = nuevoId();
        EntradaResponse e = entradaComprada(evento);
        accesoService.validar(new ValidacionAccesoRequest(e.codigoQr(), evento, "Puerta 1"), 6L);

        ValidacionAccesoResponse segunda = accesoService.validar(new ValidacionAccesoRequest(e.codigoQr(), evento, "Puerta 2"), 6L);

        assertThat(segunda.permitido()).isFalse();
        assertThat(segunda.resultado()).isEqualTo("YA_UTILIZADA");
        assertThat(segunda.mensaje()).contains("Puerta 1");
        assertThat(segunda.ingresoAnterior()).isNotNull();
    }

    @Test
    void seRechazaUnQrDeOtroEventoOInexistente() {
        long evento = nuevoId();
        EntradaResponse e = entradaComprada(evento);

        assertThat(accesoService.validar(new ValidacionAccesoRequest(e.codigoQr(), evento + 999, null), 6L).resultado())
                .isEqualTo("OTRO_EVENTO");
        assertThat(accesoService.validar(new ValidacionAccesoRequest("no-existe", evento, null), 6L).resultado())
                .isEqualTo("INEXISTENTE");
        // la entrada sigue valida para su evento
        assertThat(accesoService.validar(new ValidacionAccesoRequest(e.codigoQr(), evento, null), 6L).permitido()).isTrue();
    }

    @Test
    void unaEntradaDevueltaNoPermiteIngresar() {
        long evento = nuevoId();
        EntradaResponse e = entradaComprada(evento);
        var entrada = entradaRepository.findById(e.id()).orElseThrow();
        entrada.devolver();
        entradaRepository.save(entrada);

        ValidacionAccesoResponse r = accesoService.validar(new ValidacionAccesoRequest(e.codigoQr(), evento, null), 6L);

        assertThat(r.permitido()).isFalse();
        assertThat(r.resultado()).isEqualTo("DEVUELTA");
    }

    @Test
    void elMismoQrEscaneadoEnVariasPuertasALaVezEntraUnaSolaVez() throws Exception {
        long evento = nuevoId();
        EntradaResponse e = entradaComprada(evento);
        int lectores = 20;
        ExecutorService pool = Executors.newFixedThreadPool(lectores);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<ValidacionAccesoResponse>> futuros = new ArrayList<>();
        for (int i = 0; i < lectores; i++) {
            String puerta = "Puerta " + i;
            futuros.add(pool.submit(() -> {
                largada.await();
                return accesoService.validar(new ValidacionAccesoRequest(e.codigoQr(), evento, puerta), 6L);
            }));
        }
        largada.countDown();
        int permitidos = 0;
        for (Future<ValidacionAccesoResponse> f : futuros) {
            if (f.get(30, TimeUnit.SECONDS).permitido()) {
                permitidos++;
            }
        }
        pool.shutdown();

        assertThat(permitidos).isEqualTo(1);
        assertThat(accesoService.resumen(evento).ingresaron()).isEqualTo(1);
        assertThat(accesoService.resumen(evento).rechazos()).isEqualTo(lectores - 1);
    }
}
