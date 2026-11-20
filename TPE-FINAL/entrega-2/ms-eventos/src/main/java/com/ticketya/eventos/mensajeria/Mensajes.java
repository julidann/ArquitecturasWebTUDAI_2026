package com.ticketya.eventos.mensajeria;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mensajes de dominio que publica ms-eventos en el exchange "ticketya.eventos" (contrato documentado
 * en docs/02-arquitectura.md). Los consumen ms-fila, ms-recomendaciones, ms-notificaciones y ms-compras.
 */
public final class Mensajes {

    public static final String PUBLICADO = "evento.publicado";
    public static final String CANCELADO = "evento.cancelado";
    public static final String REPROGRAMADO = "evento.reprogramado";

    private Mensajes() {
    }

    public record EventoPublicado(Long eventoId, String nombre, String tipo, Long organizadorId,
                                  List<String> participantes, String lugar, String ciudad, LocalDateTime fechaHora,
                                  boolean altaDemanda, BigDecimal precioMinimo) {
    }

    public record EventoCancelado(Long eventoId, String nombre, String motivo) {
    }

    public record EventoReprogramado(Long eventoId, String nombre, LocalDateTime fechaAnterior,
                                     LocalDateTime fechaNueva, String motivo) {
    }
}
