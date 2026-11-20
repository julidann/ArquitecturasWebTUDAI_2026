package com.ticketya.compras.client;

import java.math.BigDecimal;
import java.util.List;

public record DisponibilidadDto(Long eventoId, String nombreEvento, List<Sector> sectores) {

    public record Sector(Long sectorId, String nombre, String tipo, BigDecimal precio, Integer capacidad,
                         Integer disponibles) {
    }
}
