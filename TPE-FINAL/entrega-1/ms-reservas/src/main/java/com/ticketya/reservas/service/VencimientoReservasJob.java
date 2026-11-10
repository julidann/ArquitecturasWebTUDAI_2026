package com.ticketya.reservas.service;

import com.ticketya.reservas.model.EstadoReserva;
import com.ticketya.reservas.repository.ReservaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Libera las reservas que vencieron sin pagarse. Si hay varias instancias corriendo el
 * job en paralelo no pasa nada: liberar() usa un UPDATE condicional y solo una instancia
 * logra pasar cada reserva de PENDIENTE a VENCIDA.
 */
@Component
public class VencimientoReservasJob {

    private static final Logger log = LoggerFactory.getLogger(VencimientoReservasJob.class);
    private static final int LOTE = 200;

    private final ReservaRepository reservaRepository;
    private final ReservaService reservaService;
    private final Clock clock;

    public VencimientoReservasJob(ReservaRepository reservaRepository, ReservaService reservaService, Clock clock) {
        this.reservaRepository = reservaRepository;
        this.reservaService = reservaService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${reservas.intervalo-vencimiento-ms:30000}")
    public void liberarVencidas() {
        List<Long> vencidas = reservaRepository.idsVencidas(LocalDateTime.now(clock), PageRequest.of(0, LOTE));
        for (Long id : vencidas) {
            try {
                reservaService.liberar(id, EstadoReserva.VENCIDA);
            } catch (RuntimeException e) {
                log.warn("No se pudo vencer la reserva {}: {}", id, e.getMessage());
            }
        }
        if (!vencidas.isEmpty()) {
            log.info("Se vencieron {} reservas", vencidas.size());
        }
    }
}
