package com.ticketya.notificaciones.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Reintenta cada tanto las notificaciones que quedaron PENDIENTES porque fallo el envio. */
@Component
public class ReenvioJob {

    private static final Logger log = LoggerFactory.getLogger(ReenvioJob.class);

    private final NotificacionService service;

    public ReenvioJob(NotificacionService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${notificaciones.intervalo-reenvio-ms:15000}",
            initialDelayString = "${notificaciones.intervalo-reenvio-ms:15000}")
    public void reenviar() {
        try {
            int n = service.reenviarPendientes();
            if (n > 0) {
                log.info("Se reintentaron {} notificaciones pendientes", n);
            }
        } catch (RuntimeException e) {
            log.warn("Fallo el job de reenvio: {}", e.getMessage());
        }
    }
}
