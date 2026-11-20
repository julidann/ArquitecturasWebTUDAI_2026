package com.ticketya.integracion;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.ticketya.integracion.Api.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Muchos clientes intentan reservar EL MISMO asiento al mismo tiempo. En docker-compose ms-reservas
 * corre con 2 instancias, asi que se prueba que el mecanismo funciona entre instancias distintas.
 */
class NoDobleVentaIT extends IntegracionBase {

    @Test
    void elMismoAsientoSeReservaUnaSolaVezAunqueLoPidanTreintaClientesALaVez() throws Exception {
        long[] lugar = lugar("Tandil", 1, 5, 10);
        long evento = eventoPublicado(organizador(), lugar, "Recital concurrencia IT", List.of("Banda"),
                LocalDateTime.now().plusDays(20), false);
        int asiento = asientosLibres(evento, lugar[1]).get(0);
        int n = 30;
        List<String> clientes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            clientes.add(clienteNuevo());
        }

        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (String c : clientes) {
            resultados.add(pool.submit(() -> {
                largada.await();
                return reservar(c, evento, lugar[1], List.of(asiento)).statusCode();
            }));
        }
        largada.countDown();
        List<Integer> codigos = new ArrayList<>();
        for (Future<Integer> f : resultados) {
            codigos.add(f.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(codigos).filteredOn(c -> c == 201).hasSize(1);
        assertThat(codigos).filteredOn(c -> c == 409).hasSize(n - 1);
    }
}
