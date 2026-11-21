package com.ticketya.notificaciones.dto;

import com.ticketya.notificaciones.model.EstadoNotificacion;
import com.ticketya.notificaciones.model.Notificacion;
import com.ticketya.notificaciones.model.TipoNotificacion;

import java.time.LocalDateTime;

public record NotificacionResponse(String id, TipoNotificacion tipo, String canal, Long eventoId, String asunto,
                                   String mensaje, EstadoNotificacion estado, int intentos, LocalDateTime creada,
                                   LocalDateTime enviada) {

    public static NotificacionResponse de(Notificacion n) {
        return new NotificacionResponse(n.getId(), n.getTipo(), n.getCanal(), n.getEventoId(), n.getAsunto(),
                n.getMensaje(), n.getEstado(), n.getIntentos(), n.getCreada(), n.getEnviada());
    }
}
