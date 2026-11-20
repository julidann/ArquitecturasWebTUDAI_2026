package com.ticketya.reservas.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReprogramacionRequest(@NotNull LocalDateTime fechaEvento) {
}
