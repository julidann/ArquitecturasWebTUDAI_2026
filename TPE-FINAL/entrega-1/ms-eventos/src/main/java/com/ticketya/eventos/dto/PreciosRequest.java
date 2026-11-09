package com.ticketya.eventos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PreciosRequest(@NotEmpty @Valid List<PrecioSectorRequest> precios) {
}
