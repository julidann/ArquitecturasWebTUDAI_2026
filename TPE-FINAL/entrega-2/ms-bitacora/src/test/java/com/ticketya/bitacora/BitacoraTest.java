package com.ticketya.bitacora;

import com.ticketya.bitacora.dto.RegistroResponse;
import com.ticketya.bitacora.mensajeria.BitacoraListener;
import com.ticketya.bitacora.mensajeria.RegistroMensaje;
import com.ticketya.bitacora.repository.RegistroRepository;
import com.ticketya.bitacora.service.BitacoraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class BitacoraTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    BitacoraListener listener;
    @Autowired
    BitacoraService service;
    @Autowired
    RegistroRepository repository;

    final LocalDateTime t = LocalDateTime.of(2026, 11, 18, 20, 0);

    @BeforeEach
    void cargar() {
        repository.deleteAll();
        // una compra que pasa por 3 servicios con el mismo correlationId
        listener.recibir(new RegistroMensaje("abc", "ms-pagos", "pago.cobro", "APROBADO", 4L,
                Map.of("compraId", 7, "monto", 90000), t.plusSeconds(1)));
        listener.recibir(new RegistroMensaje("abc", "ms-reservas", "reserva.confirmada", "OK", 4L,
                Map.of("reservaId", 3), t.plusSeconds(2)));
        listener.recibir(new RegistroMensaje("abc", "ms-compras", "compra.aprobada", "APROBADA", 4L,
                Map.of("compraId", 7, "entradas", 2), t.plusSeconds(3)));
        listener.recibir(new RegistroMensaje("xyz", "ms-compras", "compra.rechazada", "RECHAZADA", 5L,
                Map.of("compraId", 8), t.plusSeconds(4)));
        listener.recibir(new RegistroMensaje("qqq", "ms-compras", "acceso.validado", "PERMITIDO", 6L,
                Map.of("entradaId", 1), t.plusSeconds(5)));
    }

    @Test
    void conElCorrelationIdSeSigueLaOperacionEntreServiciosEnOrden() {
        assertThat(service.traza("abc")).extracting(RegistroResponse::servicio)
                .containsExactly("ms-pagos", "ms-reservas", "ms-compras");
    }

    @Test
    void seFiltraPorOperacionCompletaOPorPrefijo() {
        var filtro = new BitacoraService.Filtro(null, "compra", null, null, null, null, 50);
        assertThat(service.buscar(filtro)).extracting(RegistroResponse::operacion)
                .containsExactly("compra.rechazada", "compra.aprobada");
        var exacta = new BitacoraService.Filtro(null, "acceso.validado", null, null, null, null, 50);
        assertThat(service.buscar(exacta)).hasSize(1);
    }

    @Test
    void seFiltraPorUsuarioYRangoDeFechas() {
        var filtro = new BitacoraService.Filtro(null, null, null, 4L, t.plusSeconds(2), t.plusSeconds(10), 50);
        assertThat(service.buscar(filtro)).extracting(RegistroResponse::operacion)
                .containsExactly("compra.aprobada", "reserva.confirmada");
    }

    @Test
    void elDetalleSeGuardaTalCualLlega() {
        RegistroResponse r = service.traza("abc").get(0);
        assertThat(r.detalle()).containsEntry("monto", 90000);
    }
}
