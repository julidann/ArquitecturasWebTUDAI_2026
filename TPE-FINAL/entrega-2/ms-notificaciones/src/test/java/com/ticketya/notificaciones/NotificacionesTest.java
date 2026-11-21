package com.ticketya.notificaciones;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.ticketya.notificaciones.mensajeria.EventosListener;
import com.ticketya.notificaciones.mensajeria.Mensajes;
import com.ticketya.notificaciones.model.EstadoNotificacion;
import com.ticketya.notificaciones.model.Notificacion;
import com.ticketya.notificaciones.model.TipoNotificacion;
import com.ticketya.notificaciones.repository.NotificacionRepository;
import com.ticketya.notificaciones.service.NotificacionService;
import com.ticketya.notificaciones.service.ProveedorNotificaciones;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Notificaciones contra un MongoDB real (Testcontainers). ms-usuarios, ms-compras y
 * ms-recomendaciones se simulan con WireMock (se usa el cliente REST real).
 */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class NotificacionesTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    static final WireMockServer otros = new WireMockServer(options().dynamicPort());

    static {
        otros.start();
    }

    @DynamicPropertySource
    static void urls(DynamicPropertyRegistry r) {
        r.add("servicios.usuarios.url", otros::baseUrl);
        r.add("servicios.compras.url", otros::baseUrl);
        r.add("servicios.recomendaciones.url", otros::baseUrl);
    }

    @AfterAll
    static void fin() {
        otros.stop();
    }

    @Autowired
    EventosListener listener;
    @Autowired
    NotificacionService service;
    @Autowired
    NotificacionRepository repository;
    @Autowired
    ProveedorNotificaciones proveedor;
    @Autowired
    CircuitBreakerRegistry circuitos;

    final LocalDateTime fecha = LocalDateTime.of(2026, 12, 20, 21, 0);

    @BeforeEach
    void preparar() {
        repository.deleteAll();
        otros.resetAll();
        circuitos.getAllCircuitBreakers().forEach(c -> c.reset());
        for (long id = 4; id <= 6; id++) {
            otros.stubFor(get("/internal/usuarios/" + id).willReturn(okJson(
                    "{\"id\": " + id + ", \"email\": \"usuario" + id + "@mail.com\", \"nombre\": \"U\", \"activo\": true}")));
        }
        otros.stubFor(get("/internal/usuarios/9").willReturn(okJson(
                "{\"id\": 9, \"email\": \"baja@mail.com\", \"nombre\": \"B\", \"activo\": false}")));
    }

    @AfterEach
    void restaurar() {
        proveedor.setTasaFallos(0);
    }

    private Mensajes.CompraConfirmada compra(long compraId, long usuario) {
        return new Mensajes.CompraConfirmada(compraId, usuario, 10L, "Los Piojos", fecha, "Campo", 2,
                new BigDecimal("60000"));
    }

    @Test
    void alConfirmarUnaCompraSeEnviaUnEmailAlComprador() {
        listener.compraConfirmada(compra(1, 4));

        List<Notificacion> n = repository.findAll();
        assertThat(n).hasSize(1);
        assertThat(n.get(0).getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(n.get(0).getDestino()).isEqualTo("usuario4@mail.com");
        assertThat(n.get(0).getTipo()).isEqualTo(TipoNotificacion.COMPRA_CONFIRMADA);
        assertThat(n.get(0).getMensaje()).contains("2 entrada(s)", "20/12/2026 21:00");
    }

    @Test
    void siElMensajeLlegaDosVecesNoSeMandaDosVeces() {
        listener.compraConfirmada(compra(2, 4));
        listener.compraConfirmada(compra(2, 4));

        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void laCancelacionSeAvisaATodosLosCompradores() {
        otros.stubFor(get("/internal/eventos/10/compradores").willReturn(okJson("[4, 5, 6]")));

        listener.eventoCancelado(new Mensajes.EventoCancelado(10L, "Los Piojos", "mal clima"));

        assertThat(repository.findByEventoId(10L)).hasSize(3)
                .allMatch(n -> n.getEstado() == EstadoNotificacion.ENVIADA)
                .allMatch(n -> n.getMensaje().contains("mal clima"));
    }

    @Test
    void elCambioDeFechaSeAvisaYUnaSegundaReprogramacionTambien() {
        otros.stubFor(get("/internal/eventos/10/compradores").willReturn(okJson("[4]")));

        listener.eventoReprogramado(new Mensajes.EventoReprogramado(10L, "Los Piojos", fecha, fecha.plusDays(7), "x"));
        listener.eventoReprogramado(new Mensajes.EventoReprogramado(10L, "Los Piojos", fecha.plusDays(7),
                fecha.plusDays(14), "x"));

        assertThat(repository.findByEventoId(10L)).hasSize(2);
    }

    @Test
    void unArtistaFavoritoQuePublicaEnSuCiudadSeAvisaALosInteresados() {
        otros.stubFor(get(urlPathEqualTo("/internal/preferencias/interesados"))
                .withQueryParam("ciudad", equalTo("Tandil")).willReturn(okJson("[5, 6]")));

        listener.eventoPublicado(new Mensajes.EventoPublicado(11L, "Los Piojos en Tandil", "RECITAL", 2L,
                List.of("Los Piojos"), "Estadio", "Tandil", fecha, false, BigDecimal.TEN));

        assertThat(repository.findByEventoId(11L)).extracting(Notificacion::getUsuarioId)
                .containsExactlyInAnyOrder(5L, 6L);
        assertThat(repository.findByEventoId(11L)).allMatch(n -> n.getTipo() == TipoNotificacion.ARTISTA_FAVORITO);
    }

    @Test
    void siElProveedorEstaCaidoQuedaPendienteYDespuesSeReintenta() {
        proveedor.setTasaFallos(1);
        listener.compraConfirmada(compra(3, 4));
        Notificacion n = repository.findAll().get(0);
        assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(n.getIntentos()).isEqualTo(1);
        assertThat(n.getProximoIntento()).isAfter(LocalDateTime.now());

        proveedor.setTasaFallos(0);
        service.intentarEnviar(repository.findById(n.getId()).orElseThrow());

        Notificacion reintentada = repository.findById(n.getId()).orElseThrow();
        assertThat(reintentada.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(reintentada.getIntentos()).isEqualTo(2);
    }

    @Test
    void siSeAgotanLosIntentosQuedaFallida() {
        proveedor.setTasaFallos(1);
        listener.compraConfirmada(compra(4, 4));
        service.intentarEnviar(repository.findAll().get(0));

        assertThat(repository.findAll().get(0).getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
    }

    @Test
    void siMsUsuariosNoRespondeLaNotificacionNoSePierde() {
        otros.stubFor(get("/internal/usuarios/5").willReturn(serviceUnavailable()));

        listener.compraConfirmada(compra(5, 5));

        Notificacion n = repository.findAll().get(0);
        assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        otros.verify(3, getRequestedFor(urlEqualTo("/internal/usuarios/5"))); // reintentos de Resilience4j
    }

    @Test
    void aUnUsuarioDeshabilitadoNoSeLeEnvia() {
        listener.compraConfirmada(compra(6, 9));

        assertThat(repository.findAll().get(0).getEstado()).isEqualTo(EstadoNotificacion.DESCARTADA);
    }

    @Test
    void cadaUsuarioVeSoloSusNotificaciones() {
        listener.compraConfirmada(compra(7, 4));
        listener.compraConfirmada(compra(8, 5));
        listener.compraConfirmada(compra(9, 4));

        assertThat(service.deUsuario(4L, 20)).hasSize(2);
        assertThat(service.deUsuario(5L, 20)).hasSize(1);
    }
}
