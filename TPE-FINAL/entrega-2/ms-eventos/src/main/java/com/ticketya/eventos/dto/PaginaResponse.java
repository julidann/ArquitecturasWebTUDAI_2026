package com.ticketya.eventos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resultado paginado")
public record PaginaResponse<T>(
        List<T> contenido,
        @Schema(example = "0") int pagina,
        @Schema(example = "10") int tamanio,
        @Schema(example = "23") long totalElementos,
        @Schema(example = "3") int totalPaginas) {
}
