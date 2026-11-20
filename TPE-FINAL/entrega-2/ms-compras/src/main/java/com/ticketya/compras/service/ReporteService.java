package com.ticketya.compras.service;

import com.ticketya.compras.client.DisponibilidadDto;
import com.ticketya.compras.client.EventoDto;
import com.ticketya.compras.client.EventosClient;
import com.ticketya.compras.client.ReservasClient;
import com.ticketya.compras.dto.ReporteEventoResponse;
import com.ticketya.compras.exception.AccesoDenegadoException;
import com.ticketya.compras.model.EstadoEntrada;
import com.ticketya.compras.repository.EntradaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Reportes para organizadores (funcionalidad 12): entradas vendidas y devueltas por sector, porcentaje
 * de ocupacion y recaudacion. Los datos de ventas son de ms-compras (fuente de verdad); la capacidad de
 * cada sector se pide a ms-reservas. Si ms-reservas no responde, el reporte sale igual sin la ocupacion
 * (degradacion controlada).
 *
 * Se calcula en el momento con una consulta agregada (GROUP BY sector, estado): no se cachea porque el
 * organizador lo mira justamente para seguir la venta en vivo.
 */
@Service
public class ReporteService {

    private static final Logger log = LoggerFactory.getLogger(ReporteService.class);

    private final EntradaRepository entradaRepository;
    private final EventosClient eventosClient;
    private final ReservasClient reservasClient;
    private final Clock clock;

    public ReporteService(EntradaRepository entradaRepository, EventosClient eventosClient,
                          ReservasClient reservasClient, Clock clock) {
        this.entradaRepository = entradaRepository;
        this.eventosClient = eventosClient;
        this.reservasClient = reservasClient;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ReporteEventoResponse reporte(Long eventoId, Long usuarioId, String rol) {
        EventoDto evento = eventosClient.obtener(eventoId);
        if (!"ADMIN".equals(rol) && !Objects.equals(evento.organizadorId(), usuarioId)) {
            throw new AccesoDenegadoException("Solo el organizador del evento o un administrador pueden ver el reporte");
        }

        Map<Long, Acumulado> porSector = new TreeMap<>();
        for (Object[] fila : entradaRepository.totalesPorSectorYEstado(eventoId)) {
            Long sectorId = (Long) fila[0];
            EstadoEntrada estado = (EstadoEntrada) fila[1];
            long cantidad = ((Number) fila[2]).longValue();
            BigDecimal monto = fila[3] == null ? BigDecimal.ZERO : (BigDecimal) fila[3];
            porSector.computeIfAbsent(sectorId, k -> new Acumulado()).sumar(estado, cantidad, monto);
        }

        Map<Long, DisponibilidadDto.Sector> capacidades = new HashMap<>();
        boolean conOcupacion = true;
        try {
            DisponibilidadDto disp = reservasClient.disponibilidad(eventoId);
            disp.sectores().forEach(s -> capacidades.put(s.sectorId(), s));
            disp.sectores().forEach(s -> porSector.computeIfAbsent(s.sectorId(), k -> new Acumulado()));
        } catch (RuntimeException e) {
            log.warn("Reporte del evento {} sin ocupación: ms-reservas no respondió ({})", eventoId, e.getMessage());
            conOcupacion = false;
        }

        List<ReporteEventoResponse.Sector> sectores = new ArrayList<>();
        Acumulado total = new Acumulado();
        int capacidadTotal = 0;
        for (Map.Entry<Long, Acumulado> e : porSector.entrySet()) {
            DisponibilidadDto.Sector info = capacidades.get(e.getKey());
            Integer capacidad = info == null ? null : info.capacidad();
            Acumulado a = e.getValue();
            sectores.add(new ReporteEventoResponse.Sector(e.getKey(), info == null ? null : info.nombre(), capacidad,
                    a.vendidas(), a.devueltas, a.canceladas, a.vigentes(), a.utilizadas, porcentaje(a.vigentes(), capacidad),
                    a.recaudacion, a.reintegrado));
            total.sumarTodo(a);
            capacidadTotal += capacidad == null ? 0 : capacidad;
        }
        Integer capTotal = conOcupacion ? capacidadTotal : null;
        return new ReporteEventoResponse(eventoId, evento.nombre(), evento.organizadorId(), LocalDateTime.now(clock),
                conOcupacion, sectores,
                new ReporteEventoResponse.Totales(capTotal, total.vendidas(), total.devueltas, total.canceladas,
                        total.vigentes(), total.utilizadas, porcentaje(total.vigentes(), capTotal), total.recaudacion,
                        total.reintegrado));
    }

    private static Double porcentaje(long vigentes, Integer capacidad) {
        if (capacidad == null || capacidad == 0) {
            return null;
        }
        return BigDecimal.valueOf(vigentes * 100.0 / capacidad).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static final class Acumulado {
        long validas;
        long utilizadas;
        long devueltas;
        long canceladas;
        BigDecimal recaudacion = BigDecimal.ZERO;
        BigDecimal reintegrado = BigDecimal.ZERO;

        void sumar(EstadoEntrada estado, long cantidad, BigDecimal monto) {
            switch (estado) {
                case VALIDA -> { validas += cantidad; recaudacion = recaudacion.add(monto); }
                case UTILIZADA -> { utilizadas += cantidad; recaudacion = recaudacion.add(monto); }
                case DEVUELTA -> { devueltas += cantidad; reintegrado = reintegrado.add(monto); }
                case CANCELADA -> { canceladas += cantidad; reintegrado = reintegrado.add(monto); }
            }
        }

        void sumarTodo(Acumulado o) {
            validas += o.validas;
            utilizadas += o.utilizadas;
            devueltas += o.devueltas;
            canceladas += o.canceladas;
            recaudacion = recaudacion.add(o.recaudacion);
            reintegrado = reintegrado.add(o.reintegrado);
        }

        long vigentes() { return validas + utilizadas; }
        long vendidas() { return validas + utilizadas + devueltas + canceladas; }
    }
}
