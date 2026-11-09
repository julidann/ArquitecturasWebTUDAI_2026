package com.ticketya.eventos.service;

import com.ticketya.eventos.dto.EventoResponse;
import com.ticketya.eventos.dto.FiltroBusqueda;
import com.ticketya.eventos.dto.OrdenBusqueda;
import com.ticketya.eventos.dto.PaginaResponse;
import com.ticketya.eventos.model.*;
import com.ticketya.eventos.repository.EventoRepository;
import com.ticketya.eventos.repository.LugarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba la busqueda contra una base H2 real para validar que las Specifications
 * generen bien las consultas (con mocks no tendria mucho sentido).
 */
@DataJpaTest
@Import(BusquedaService.class)
class BusquedaServiceTest {

    @Autowired
    private BusquedaService busquedaService;
    @Autowired
    private EventoRepository eventoRepository;
    @Autowired
    private LugarRepository lugarRepository;

    private final LocalDateTime base = LocalDate.now().plusMonths(2).atTime(21, 0);

    @BeforeEach
    void cargarDatos() {
        Lugar estadio = new Lugar("Estadio Único", "Av. 32", "La Plata", null, null);
        estadio.agregarSector(Sector.noNumerado("Campo", 1000));
        Lugar teatro = new Lugar("Teatro del Lago", "Av. Colón 1234", "Tandil", null, null);
        teatro.agregarSector(Sector.noNumerado("Sala", 300));
        lugarRepository.saveAll(List.of(estadio, teatro));

        crear("Los Piojos - Ritual", "Vuelven los Piojos", TipoEvento.RECITAL, 10L, List.of("Los Piojos"),
                base.plusDays(10), estadio, "60000", EstadoEvento.PUBLICADO);
        crear("Festival de Rock", "Con La Renga y Los Piojos como invitados", TipoEvento.FESTIVAL, 11L,
                List.of("La Renga", "Los Piojos"), base.plusDays(3), estadio, "45000", EstadoEvento.PUBLICADO);
        crear("Hamlet", "Clásico de Shakespeare", TipoEvento.TEATRO, 12L, List.of("Elenco Municipal"),
                base.plusDays(1), teatro, "15000", EstadoEvento.AGOTADO);
        crear("Charla de Arquitectura", "Microservicios en la práctica", TipoEvento.CONFERENCIA, 12L,
                List.of("Juan Pérez"), base.plusDays(20), teatro, "5000", EstadoEvento.PUBLICADO);
        // no deberian aparecer nunca
        crear("Borrador secreto", "Piojos", TipoEvento.RECITAL, 10L, List.of("Los Piojos"),
                base.plusDays(5), estadio, "1000", EstadoEvento.BORRADOR);
        crear("Evento cancelado", "Piojos", TipoEvento.RECITAL, 10L, List.of("Los Piojos"),
                base.plusDays(6), estadio, "1000", EstadoEvento.CANCELADO);
    }

    private void crear(String nombre, String desc, TipoEvento tipo, Long org, List<String> participantes,
                       LocalDateTime fecha, Lugar lugar, String precio, EstadoEvento estado) {
        Evento e = new Evento(nombre, desc, tipo, org, participantes, fecha, lugar);
        e.definirPrecio(lugar.getSectores().get(0), new BigDecimal(precio));
        e.cambiarEstado(estado);
        eventoRepository.save(e);
    }

    private FiltroBusqueda filtro(String texto, TipoEvento tipo, Long org, String participante, String ciudad,
                                  LocalDate desde, LocalDate hasta, OrdenBusqueda orden, boolean desc) {
        return new FiltroBusqueda(texto, tipo, org, participante, ciudad, desde, hasta, orden, desc, 0, 10);
    }

    private List<String> nombres(PaginaResponse<EventoResponse> r) {
        return r.contenido().stream().map(EventoResponse::nombre).toList();
    }

    @Test
    void sinFiltrosDevuelveSoloPublicadosYAgotadosOrdenadosPorFecha() {
        var r = busquedaService.buscar(filtro(null, null, null, null, null, null, null, OrdenBusqueda.FECHA, false));

        assertThat(nombres(r)).containsExactly("Hamlet", "Festival de Rock", "Los Piojos - Ritual",
                "Charla de Arquitectura");
        assertThat(r.totalElementos()).isEqualTo(4);
    }

    @Test
    void filtraPorTipo() {
        var r = busquedaService.buscar(filtro(null, TipoEvento.TEATRO, null, null, null, null, null,
                OrdenBusqueda.FECHA, false));

        assertThat(nombres(r)).containsExactly("Hamlet");
    }

    @Test
    void filtraPorParticipanteSinImportarMayusculas() {
        var r = busquedaService.buscar(filtro(null, null, null, "los piojos", null, null, null,
                OrdenBusqueda.FECHA, false));

        assertThat(nombres(r)).containsExactly("Festival de Rock", "Los Piojos - Ritual");
    }

    @Test
    void combinaCiudadYOrganizador() {
        var r = busquedaService.buscar(filtro(null, null, 12L, null, "tandil", null, null,
                OrdenBusqueda.FECHA, false));

        assertThat(nombres(r)).containsExactly("Hamlet", "Charla de Arquitectura");
    }

    @Test
    void filtraPorRangoDeFechas() {
        LocalDate desde = base.plusDays(2).toLocalDate();
        LocalDate hasta = base.plusDays(10).toLocalDate();

        var r = busquedaService.buscar(filtro(null, null, null, null, null, desde, hasta, OrdenBusqueda.FECHA, false));

        assertThat(nombres(r)).containsExactly("Festival de Rock", "Los Piojos - Ritual");
    }

    @Test
    void ordenaPorPrecioDescendente() {
        var r = busquedaService.buscar(filtro(null, null, null, null, null, null, null, OrdenBusqueda.PRECIO, true));

        assertThat(nombres(r)).containsExactly("Los Piojos - Ritual", "Festival de Rock", "Hamlet",
                "Charla de Arquitectura");
    }

    @Test
    void ordenaPorRelevanciaPriorizandoCoincidenciasEnElNombre() {
        var r = busquedaService.buscar(filtro("piojos", null, null, null, null, null, null,
                OrdenBusqueda.RELEVANCIA, false));

        // "Los Piojos - Ritual" matchea en nombre (3) + participante (2) + descripcion (1)
        // "Festival de Rock" matchea en participante (2) + descripcion (1)
        assertThat(nombres(r)).containsExactly("Los Piojos - Ritual", "Festival de Rock");
    }

    @Test
    void laRelevanciaIgnoraTildes() {
        var r = busquedaService.buscar(filtro("practica", null, null, null, null, null, null,
                OrdenBusqueda.RELEVANCIA, false));

        assertThat(nombres(r)).containsExactly("Charla de Arquitectura");
    }

    @Test
    void paginaLosResultados() {
        var r = busquedaService.buscar(new FiltroBusqueda(null, null, null, null, null, null, null,
                OrdenBusqueda.FECHA, false, 1, 3));

        assertThat(nombres(r)).containsExactly("Charla de Arquitectura");
        assertThat(r.totalPaginas()).isEqualTo(2);
    }

    @Test
    void rangoDeFechasInvertidoEsInvalido() {
        assertThatThrownBy(() -> busquedaService.buscar(filtro(null, null, null, null, null,
                LocalDate.now().plusDays(5), LocalDate.now(), OrdenBusqueda.FECHA, false)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
