package com.ticketya.compras.service;

import com.ticketya.compras.client.DisponibilidadDto;
import com.ticketya.compras.client.EventoDto;
import com.ticketya.compras.client.ReservaDto;
import com.ticketya.compras.dto.ReporteEventoResponse;
import com.ticketya.compras.exception.AccesoDenegadoException;
import com.ticketya.compras.exception.ServicioNoDisponibleException;
import com.ticketya.compras.repository.EntradaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Reportes para organizadores (funcionalidad 12). */
@SpringBootTest
class ReporteServiceTest extends ComprasTestBase {

    static final long ORGANIZADOR = 2L;

    @Autowired
    ReporteService reporteService;
    @Autowired
    EntradaRepository entradaRepository;

    private long eventoConVentas() {
        long evento = nuevoId();
        // 2 compras de 2 entradas (sector 3, 45000 c/u) y una entrada devuelta
        ReservaDto r1 = prepararCompraAprobada(evento, LocalDateTime.now().plusDays(10));
        var c1 = comprar(r1.id());
        ReservaDto r2 = prepararCompraAprobada(evento, LocalDateTime.now().plusDays(10), 2);
        comprar(r2.id());
        var devuelta = entradaRepository.findById(c1.compra().entradas().get(0).id()).orElseThrow();
        devuelta.devolver();
        entradaRepository.save(devuelta);
        when(eventosClient.obtener(evento)).thenReturn(new EventoDto(evento, "Recital", ORGANIZADOR, "PUBLICADO",
                LocalDateTime.now().plusDays(10)));
        return evento;
    }

    @Test
    void elReporteCalculaVendidasDevueltasOcupacionYRecaudacionPorSector() {
        long evento = eventoConVentas();
        when(reservasClient.disponibilidad(evento)).thenReturn(new DisponibilidadDto(evento, "Recital", List.of(
                new DisponibilidadDto.Sector(3L, "Platea", "NUMERADO", new BigDecimal("45000"), 10, 7),
                new DisponibilidadDto.Sector(4L, "Campo", "NO_NUMERADO", new BigDecimal("30000"), 100, 100))));

        ReporteEventoResponse r = reporteService.reporte(evento, ORGANIZADOR, "ORGANIZADOR");

        assertThat(r.ocupacionDisponible()).isTrue();
        assertThat(r.sectores()).hasSize(2);
        ReporteEventoResponse.Sector platea = r.sectores().get(0);
        assertThat(platea.nombre()).isEqualTo("Platea");
        assertThat(platea.vendidas()).isEqualTo(4);
        assertThat(platea.devueltas()).isEqualTo(1);
        assertThat(platea.vigentes()).isEqualTo(3);
        assertThat(platea.porcentajeOcupacion()).isEqualTo(30.0);
        assertThat(platea.recaudacion()).isEqualByComparingTo("135000");
        assertThat(platea.reintegrado()).isEqualByComparingTo("45000");
        ReporteEventoResponse.Sector campo = r.sectores().get(1);
        assertThat(campo.vendidas()).isZero();
        assertThat(campo.porcentajeOcupacion()).isEqualTo(0.0);
        assertThat(r.totales().capacidad()).isEqualTo(110);
        assertThat(r.totales().recaudacion()).isEqualByComparingTo("135000");
    }

    @Test
    void otroOrganizadorNoPuedeVerElReportePeroUnAdminSi() {
        long evento = eventoConVentas();
        when(reservasClient.disponibilidad(evento)).thenReturn(new DisponibilidadDto(evento, "Recital", List.of()));

        assertThatThrownBy(() -> reporteService.reporte(evento, 99L, "ORGANIZADOR"))
                .isInstanceOf(AccesoDenegadoException.class);
        assertThat(reporteService.reporte(evento, 1L, "ADMIN").totales().vendidas()).isEqualTo(4);
    }

    @Test
    void siReservasNoRespondeElReporteSaleSinOcupacion() {
        long evento = eventoConVentas();
        when(reservasClient.disponibilidad(evento)).thenThrow(new ServicioNoDisponibleException("caido", null));

        ReporteEventoResponse r = reporteService.reporte(evento, ORGANIZADOR, "ORGANIZADOR");

        assertThat(r.ocupacionDisponible()).isFalse();
        assertThat(r.totales().vendidas()).isEqualTo(4);
        assertThat(r.totales().porcentajeOcupacion()).isNull();
    }
}
