package com.ticketya.compras.service;

import com.ticketya.compras.client.PagoDto;
import com.ticketya.compras.client.PagosClient;
import com.ticketya.compras.exception.ServicioNoDisponibleException;
import com.ticketya.compras.model.Compra;
import com.ticketya.compras.repository.CompraRepository;
import com.ticketya.compras.trazabilidad.CorrelacionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Manejo de fallos parciales de la saga de compra: retoma las compras que quedaron en ERROR (se corto la
 * comunicacion con la pasarela o con ms-reservas a mitad de camino) y el cliente no reintento.
 *
 * Para cada una le pregunta a la pasarela que paso con el cobro (por compraId):
 *  - no hay ningun pago registrado -> nunca se cobro: se libera la reserva y la compra queda RECHAZADA.
 *  - pago RECHAZADO -> se libera la reserva y la compra queda RECHAZADA.
 *  - pago APROBADO -> se intenta confirmar la reserva; si ya vencio, se compensa con un reintegro.
 *  - la pasarela sigue sin responder -> se deja en ERROR y se reintenta en la proxima vuelta.
 *
 * Cada compra se "reclama" con un UPDATE condicional (ERROR -> PENDIENTE), asi aunque el job corra en
 * varias instancias, o el cliente reintente justo, cada compra la procesa uno solo.
 */
@Component
public class RecuperacionComprasJob {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionComprasJob.class);

    private final CompraRepository compraRepository;
    private final CompraService compraService;
    private final PagosClient pagosClient;
    private final int segundosDeGracia;

    public RecuperacionComprasJob(CompraRepository compraRepository, CompraService compraService,
                                  PagosClient pagosClient,
                                  @Value("${compras.recuperacion.segundos-de-gracia:60}") int segundosDeGracia) {
        this.compraRepository = compraRepository;
        this.compraService = compraService;
        this.pagosClient = pagosClient;
        this.segundosDeGracia = segundosDeGracia;
    }

    @Scheduled(fixedDelayString = "${compras.recuperacion.intervalo-ms:30000}",
            initialDelayString = "${compras.recuperacion.intervalo-ms:30000}")
    public void ejecutar() {
        List<Long> ids = compraRepository.idsEnErrorAntesDe(LocalDateTime.now().minusSeconds(segundosDeGracia));
        for (Long id : ids) {
            try {
                recuperar(id);
            } catch (RuntimeException e) {
                log.warn("No se pudo recuperar la compra {}: {}", id, e.getMessage());
            }
        }
    }

    /** @return true si la compra quedo resuelta (APROBADA o RECHAZADA) */
    public boolean recuperar(Long id) {
        if (!compraService.reclamar(id)) {
            return false; // la tomo otro (cliente u otra instancia)
        }
        MDC.put(CorrelacionFilter.MDC_CLAVE, "recuperacion-compra-" + id);
        try {
            Compra compra = compraRepository.findById(id).orElseThrow();
            List<PagoDto> pagos;
            try {
                pagos = pagosClient.buscarPorCompra(id);
            } catch (ServicioNoDisponibleException e) {
                compraService.error(id, "Recuperación: la pasarela sigue sin responder");
                return false;
            }
            Optional<PagoDto> aprobado = pagos.stream().filter(PagoDto::aprobado).findFirst();
            if (aprobado.isPresent()) {
                log.info("Recuperación de la compra {}: el pago {} estaba aprobado, se completa", id, aprobado.get().id());
                compraService.continuarConPago(compra, aprobado.get());
                return compraService.respuesta(id).estado() != com.ticketya.compras.model.EstadoCompra.ERROR;
            }
            Optional<PagoDto> rechazado = pagos.stream().max(Comparator.comparing(PagoDto::id));
            if (rechazado.isPresent()) {
                log.info("Recuperación de la compra {}: el pago fue rechazado", id);
                compraService.continuarConPago(compra, rechazado.get());
                return true;
            }
            log.info("Recuperación de la compra {}: la pasarela no registra ningún cobro, se cierra sin cobrar", id);
            compraService.cerrarSinCobro(compra, "No se pudo completar el cobro (la pasarela no respondió). "
                    + "No se te cobró nada; podés volver a reservar.");
            return true;
        } finally {
            MDC.remove(CorrelacionFilter.MDC_CLAVE);
        }
    }
}
