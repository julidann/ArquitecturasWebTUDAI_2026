package com.ticketya.reservas.dto;

import com.ticketya.reservas.model.TipoSector;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Lo envia ms-eventos al publicar un evento. */
public record DisponibilidadInicialRequest(
        @NotNull Long eventoId,
        @NotNull String nombreEvento,
        @NotNull LocalDateTime fechaEvento,
        @NotNull Integer limitePorCliente,
        @NotEmpty @Valid List<SectorInicial> sectores) {

    public record SectorInicial(@NotNull Long sectorId, @NotNull String nombre, @NotNull TipoSector tipo,
                                @NotNull BigDecimal precio, @NotNull Integer capacidad,
                                List<AsientoInicial> asientos) {
    }

    public record AsientoInicial(@NotNull Long asientoId, @NotNull String fila, @NotNull Integer numero) {
    }
}
