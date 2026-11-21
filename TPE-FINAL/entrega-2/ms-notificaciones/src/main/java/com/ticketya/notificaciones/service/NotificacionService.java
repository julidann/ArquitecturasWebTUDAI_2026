package com.ticketya.notificaciones.service;

import com.ticketya.notificaciones.client.ContactoDto;
import com.ticketya.notificaciones.client.ServiciosClient;
import com.ticketya.notificaciones.dto.NotificacionResponse;
import com.ticketya.notificaciones.model.EstadoNotificacion;
import com.ticketya.notificaciones.model.Notificacion;
import com.ticketya.notificaciones.model.TipoNotificacion;
import com.ticketya.notificaciones.repository.NotificacionRepository;
import com.ticketya.notificaciones.trazabilidad.CorrelacionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

/**
 * Crea y envia notificaciones. Primero se GUARDA la notificacion (PENDIENTE) y despues se intenta
 * enviar: si el proveedor o ms-usuarios fallan, la notificacion no se pierde y el job de reenvio la
 * vuelve a intentar con espera creciente hasta un maximo de intentos.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository repository;
    private final ServiciosClient servicios;
    private final ProveedorNotificaciones proveedor;
    private final Clock clock;
    private final int maxIntentos;

    public NotificacionService(NotificacionRepository repository, ServiciosClient servicios,
                               ProveedorNotificaciones proveedor, Clock clock,
                               @Value("${notificaciones.max-intentos:5}") int maxIntentos) {
        this.repository = repository;
        this.servicios = servicios;
        this.proveedor = proveedor;
        this.clock = clock;
        this.maxIntentos = maxIntentos;
    }

    /** Crea una notificacion por destinatario (salteando las que ya existian) e intenta enviarlas. */
    public int notificar(TipoNotificacion tipo, String referencia, Collection<Long> usuarios, Long eventoId,
                         String asunto, String mensaje) {
        int nuevas = 0;
        for (Long usuarioId : usuarios) {
            Notificacion n = new Notificacion(tipo, referencia, usuarioId, eventoId, asunto, mensaje,
                    MDC.get(CorrelacionFilter.MDC_CLAVE), LocalDateTime.now(clock));
            try {
                n = repository.insert(n);
            } catch (DuplicateKeyException e) {
                log.info("La notificacion {} ya existia (mensaje repetido), no se vuelve a enviar", n.getClave());
                continue;
            }
            nuevas++;
            intentarEnviar(n);
        }
        return nuevas;
    }

    /** Un intento de envio. Nunca lanza excepcion: el resultado queda en el estado de la notificacion. */
    public void intentarEnviar(Notificacion n) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        try {
            Optional<ContactoDto> contacto = servicios.contacto(n.getUsuarioId());
            if (contacto.isEmpty() || !contacto.get().activo()) {
                n.descartar("El usuario no existe o está deshabilitado");
            } else {
                proveedor.enviar(contacto.get().email(), n.getAsunto(), n.getMensaje());
                n.marcarEnviada(contacto.get().email(), ahora);
            }
        } catch (RuntimeException e) {
            n.registrarFallo(e.getMessage(), maxIntentos, ahora);
            log.warn("No se pudo enviar la notificacion {} (intento {}): {}", n.getClave(), n.getIntentos(),
                    e.getMessage());
        }
        try {
            repository.save(n);
        } catch (OptimisticLockingFailureException e) {
            log.info("La notificacion {} la actualizo otro proceso al mismo tiempo", n.getClave());
        }
    }

    /** Lo llama el job: reintenta las pendientes cuyo proximo intento ya llego. */
    public int reenviarPendientes() {
        var pendientes = repository.findByEstadoAndProximoIntentoLessThanEqual(EstadoNotificacion.PENDIENTE,
                LocalDateTime.now(clock), PageRequest.of(0, 100));
        pendientes.forEach(this::intentarEnviar);
        return pendientes.size();
    }

    public java.util.List<NotificacionResponse> deUsuario(Long usuarioId, int limite) {
        return repository.findByUsuarioIdOrderByCreadaDesc(usuarioId, PageRequest.of(0, limite)).stream()
                .map(NotificacionResponse::de).toList();
    }
}
