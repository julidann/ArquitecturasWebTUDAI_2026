package com.ticketya.eventos.client;

import com.ticketya.eventos.model.TipoSector;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos que se le mandan a ms-reservas al publicar un evento para que arme el
 * inventario de lugares. Se copian precio y fecha para que ms-reservas no tenga
 * que consultar a ms-eventos en cada reserva (camino critico de la compra).
 */
public record DisponibilidadInicialRequest(
        Long eventoId,
        String nombreEvento,
        LocalDateTime fechaEvento,
        Integer limitePorCliente,
        List<SectorInicial> sectores) {

    public record SectorInicial(Long sectorId, String nombre, TipoSector tipo, BigDecimal precio,
                                Integer capacidad, List<AsientoInicial> asientos) {
    }

    public record AsientoInicial(Long asientoId, String fila, Integer numero) {
    }
}
