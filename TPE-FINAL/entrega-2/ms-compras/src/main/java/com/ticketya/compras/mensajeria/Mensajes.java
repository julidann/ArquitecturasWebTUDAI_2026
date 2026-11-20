package com.ticketya.compras.mensajeria;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Mensajes de dominio que publica / consume ms-compras (exchange "ticketya.eventos"). */
public final class Mensajes {

    public static final String COMPRA_CONFIRMADA = "compra.confirmada";

    private Mensajes() {
    }

    /** Lo consumen ms-notificaciones (email de confirmacion) y ms-recomendaciones (historial). */
    public record CompraConfirmada(Long compraId, Long usuarioId, Long eventoId, String nombreEvento,
                                   LocalDateTime fechaEvento, String sector, Integer cantidad, BigDecimal montoTotal) {
    }

    /** Lo publica ms-eventos al cambiar la fecha de un evento. */
    public record EventoReprogramado(Long eventoId, String nombre, LocalDateTime fechaAnterior,
                                     LocalDateTime fechaNueva, String motivo) {
    }
}
