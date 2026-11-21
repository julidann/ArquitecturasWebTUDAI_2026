package com.ticketya.notificaciones.mensajeria;

import com.ticketya.notificaciones.config.MensajeriaConfig;
import com.ticketya.notificaciones.model.TipoNotificacion;
import com.ticketya.notificaciones.client.ServiciosClient;
import com.ticketya.notificaciones.service.NotificacionService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Funcionalidad 11. Las notificaciones son asincronicas: la compra o la cancelacion no esperan a
 * que salga el email. Si para saber los destinatarios hay que consultar otro servicio y este no
 * responde, la excepcion hace que RabbitMQ reintente el mensaje (y a los 3 fallos va a la DLQ).
 */
@Component
public class EventosListener {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificacionService service;
    private final ServiciosClient servicios;

    public EventosListener(NotificacionService service, ServiciosClient servicios) {
        this.service = service;
        this.servicios = servicios;
    }

    @RabbitListener(queues = MensajeriaConfig.COLA_COMPRA)
    public void compraConfirmada(Mensajes.CompraConfirmada m) {
        service.notificar(TipoNotificacion.COMPRA_CONFIRMADA, String.valueOf(m.compraId()), List.of(m.usuarioId()),
                m.eventoId(), "Tu compra para " + m.nombreEvento() + " está confirmada",
                "Compraste " + m.cantidad() + " entrada(s) en " + m.sector() + " para " + m.nombreEvento() + " ("
                        + FECHA.format(m.fechaEvento()) + ") por $" + m.montoTotal()
                        + ". Tus códigos QR están en Mis entradas.");
    }

    @RabbitListener(queues = MensajeriaConfig.COLA_CANCELADO)
    public void eventoCancelado(Mensajes.EventoCancelado m) {
        service.notificar(TipoNotificacion.EVENTO_CANCELADO, String.valueOf(m.eventoId()),
                servicios.compradores(m.eventoId()), m.eventoId(), m.nombre() + " fue cancelado",
                "Lamentamos informarte que " + m.nombre() + " fue cancelado"
                        + (m.motivo() == null ? "" : " (" + m.motivo() + ")")
                        + ". Te reintegramos el total de tus entradas al mismo medio de pago.");
    }

    @RabbitListener(queues = MensajeriaConfig.COLA_REPROGRAMADO)
    public void eventoReprogramado(Mensajes.EventoReprogramado m) {
        service.notificar(TipoNotificacion.EVENTO_REPROGRAMADO, m.eventoId() + "@" + m.fechaNueva(),
                servicios.compradores(m.eventoId()), m.eventoId(), "Cambio de fecha: " + m.nombre(),
                m.nombre() + " pasó del " + FECHA.format(m.fechaAnterior()) + " al " + FECHA.format(m.fechaNueva())
                        + ". Tus entradas siguen siendo válidas; si no podés ir podés pedir la devolución.");
    }

    @RabbitListener(queues = MensajeriaConfig.COLA_PUBLICADO)
    public void eventoPublicado(Mensajes.EventoPublicado m) {
        if (m.participantes() == null || m.participantes().isEmpty() || m.ciudad() == null) {
            return;
        }
        List<Long> interesados = servicios.interesados(m.participantes(), m.ciudad());
        service.notificar(TipoNotificacion.ARTISTA_FAVORITO, String.valueOf(m.eventoId()), interesados, m.eventoId(),
                String.join(", ", m.participantes()) + " en " + m.ciudad(),
                "Uno de tus artistas favoritos se presenta en tu ciudad: " + m.nombre() + " el "
                        + FECHA.format(m.fechaHora()) + " en " + m.lugar() + ". ¡Ya podés comprar tus entradas!");
    }
}
