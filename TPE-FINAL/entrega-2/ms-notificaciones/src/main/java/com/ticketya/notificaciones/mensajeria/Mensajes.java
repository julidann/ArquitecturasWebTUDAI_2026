package com.ticketya.notificaciones.mensajeria;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Mensajes que consume este servicio (contratos en docs/02-arquitectura.md). */
public final class Mensajes {

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

    public record CompraConfirmada(Long compraId, Long usuarioId, Long eventoId, String nombreEvento,
                                   LocalDateTime fechaEvento, String sector, Integer cantidad, BigDecimal montoTotal) {
    }
}
