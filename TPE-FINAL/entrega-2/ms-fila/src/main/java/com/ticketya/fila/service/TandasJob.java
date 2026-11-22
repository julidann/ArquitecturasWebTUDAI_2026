package com.ticketya.fila.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Revisa cada segundo las filas abiertas y habilita la siguiente tanda cuando corresponde. */
@Component
public class TandasJob {

    private static final Logger log = LoggerFactory.getLogger(TandasJob.class);
    private final FilaService filaService;

    public TandasJob(FilaService filaService) {
        this.filaService = filaService;
    }

    @Scheduled(fixedDelayString = "${fila.intervalo-job-ms:1000}", initialDelayString = "${fila.intervalo-job-ms:1000}")
    public void ejecutar() {
        try {
            for (Long eventoId : filaService.filasActivas()) {
                filaService.habilitarTanda(eventoId);
            }
        } catch (RuntimeException e) {
            // si Redis no responde no queremos matar el scheduler: se reintenta en el proximo ciclo
            log.warn("No se pudieron procesar las tandas: {}", e.getMessage());
        }
    }
}
