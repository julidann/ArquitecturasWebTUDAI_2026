package com.ticketya.reservas.dto;

import jakarta.validation.constraints.NotNull;

public record HabilitacionRequest(@NotNull Boolean habilitado, String motivo) {
}
