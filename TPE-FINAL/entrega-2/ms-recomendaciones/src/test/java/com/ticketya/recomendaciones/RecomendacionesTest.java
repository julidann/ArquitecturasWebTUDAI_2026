package com.ticketya.recomendaciones;

import com.ticketya.recomendaciones.dto.PreferenciasRequest;
import com.ticketya.recomendaciones.dto.RecomendacionResponse;
import com.ticketya.recomendaciones.dto.TambienCompraronResponse;
import com.ticketya.recomendaciones.mensajeria.EventosListener;
import com.ticketya.recomendaciones.mensajeria.Mensajes;
import com.ticketya.recomendaciones.repository.CompraHistorialRepository;
import com.ticketya.recomendaciones.service.PreferenciasService;
import com.ticketya.recomendaciones.service.RecomendacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Preferencias y recomendaciones contra un MongoDB real (Testcontainers). */
@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class RecomendacionesTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    EventosListener listener;
    @Autowired
    PreferenciasService preferencias;
    @Autowired
    RecomendacionService recomendaciones;
    @Autowired
    CompraHistorialRepository historial;
    @Autowired
    MongoTemplate template;

    private final LocalDateTime enUnMes = LocalDateTime.now().plusDays(30);
    private long compras = 1;

    @BeforeEach
    void limpiar() {
        template.getDb().drop();
        // eventos: 1 y 2 de Los Piojos (Tandil y CABA), 3 teatro en Tandil, 4 recital pasado, 5 cancelado
        publicar(1, "Los Piojos en Tandil", "RECITAL", List.of("Los Piojos"), "Tandil", enUnMes);
        publicar(2, "Los Piojos en CABA", "RECITAL", List.of("Los Piojos", "Ciro"), "CABA", enUnMes.plusDays(1));
        publicar(3, "Hamlet", "TEATRO", List.of("Elenco Municipal"), "Tandil", enUnMes.plusDays(2));
        publicar(4, "Recital viejo", "RECITAL", List.of("Los Piojos"), "Tandil", LocalDateTime.now().minusDays(3));
        publicar(5, "Fito cancelado", "RECITAL", List.of("Fito Paez"), "Tandil", enUnMes);
        listener.cancelado(new Mensajes.EventoCancelado(5L, "Fito cancelado", "lluvia"));
    }

    private void publicar(long id, String nombre, String tipo, List<String> artistas, String ciudad, LocalDateTime f) {
        listener.publicado(new Mensajes.EventoPublicado(id, nombre, tipo, 2L, artistas, "Lugar " + id, ciudad, f,
                false, new BigDecimal("10000")));
    }

    private void compra(long usuario, long evento) {
        listener.compra(new Mensajes.CompraConfirmada(compras++, usuario, evento, "x", enUnMes, "Campo", 1,
                BigDecimal.TEN));
    }

    @Test
    void lasPreferenciasSeGuardanYSeReemplazanCompletas() {
        preferencias.guardar(10L, new PreferenciasRequest(List.of("recital", "RECITAL"), List.of(" Los Piojos "), "Tandil"));
        var p = preferencias.guardar(10L, new PreferenciasRequest(List.of("teatro"), List.of("Ciro"), null));

        assertThat(p.tiposEvento()).containsExactly("TEATRO");
        assertThat(p.artistasFavoritos()).containsExactly("Ciro");
        assertThat(p.ciudad()).isNull();
        assertThat(preferencias.obtener(99L).artistasFavoritos()).isEmpty();
    }

    @Test
    void seEncuentranLosInteresadosEnUnArtistaEnSuCiudadSinImportarMayusculas() {
        preferencias.guardar(10L, new PreferenciasRequest(null, List.of("los piojos"), "tandil"));
        preferencias.guardar(11L, new PreferenciasRequest(null, List.of("Los Piojos"), "CABA"));
        preferencias.guardar(12L, new PreferenciasRequest(null, List.of("Ciro", "Fito Paez"), "Tandil"));

        assertThat(preferencias.interesados(List.of("Los Piojos"), "Tandil")).containsExactly(10L);
        assertThat(preferencias.interesados(List.of("LOS PIOJOS", "Ciro"), "Tandil")).containsExactly(10L, 12L);
        assertThat(preferencias.interesados(List.of("Nadie"), "Tandil")).isEmpty();
    }

    @Test
    void quienesCompraronEsteEventoTambienCompraron() {
        compra(20, 1); compra(20, 3);
        compra(21, 1); compra(21, 3);
        compra(22, 1); compra(22, 2);
        compra(23, 1); compra(23, 5); // el 5 esta cancelado: no se sugiere

        List<TambienCompraronResponse> r = recomendaciones.tambienCompraron(1L, 5);

        assertThat(r).extracting(TambienCompraronResponse::eventoId).containsExactly(3L, 2L);
        assertThat(r.get(0).compradoresEnComun()).isEqualTo(2);
    }

    @Test
    void lasRecomendacionesCombinanPreferenciasEHistorialYNoRepitenLoComprado() {
        preferencias.guardar(10L, new PreferenciasRequest(List.of("TEATRO"), List.of("Los Piojos"), "Tandil"));
        compra(10, 1);              // ya fue al de Tandil
        compra(30, 1); compra(30, 3); // otro cliente que fue al 1 tambien fue a Hamlet

        List<RecomendacionResponse> r = recomendaciones.recomendar(10L, 10);

        // 2: artista favorito (5) + recital como el que compro (1) = 6
        // 3: co-compra (3) + teatro en su ciudad (2) = 5
        assertThat(r).extracting(RecomendacionResponse::eventoId).containsExactly(2L, 3L);
        assertThat(r.get(0).puntaje()).isEqualTo(6);
        assertThat(r.get(0).motivos()).anyMatch(m -> m.contains("Los Piojos"));
        assertThat(r.get(1).puntaje()).isEqualTo(5);
        // nunca se sugiere lo ya comprado (1), lo pasado (4) ni lo cancelado (5)
        assertThat(r).extracting(RecomendacionResponse::eventoId).doesNotContain(1L, 4L, 5L);
    }

    @Test
    void unClienteSinDatosNoRecibeSugerencias() {
        assertThat(recomendaciones.recomendar(77L, 10)).isEmpty();
    }

    @Test
    void siElMensajeDeCompraLlegaDosVecesNoSeDuplicaElHistorial() {
        var m = new Mensajes.CompraConfirmada(500L, 40L, 1L, "x", enUnMes, "Campo", 2, BigDecimal.TEN);
        listener.compra(m);
        listener.compra(m);

        assertThat(historial.findByUsuarioId(40L)).hasSize(1);
    }

    @Test
    void laReprogramacionActualizaLaFechaDelCatalogo() {
        LocalDateTime nueva = enUnMes.plusDays(60).withNano(0);
        listener.reprogramado(new Mensajes.EventoReprogramado(3L, "Hamlet", enUnMes, nueva, "agenda"));

        preferencias.guardar(10L, new PreferenciasRequest(List.of("TEATRO"), null, "Tandil"));
        assertThat(recomendaciones.recomendar(10L, 5).get(0).fechaHora()).isEqualTo(nueva);
    }
}
