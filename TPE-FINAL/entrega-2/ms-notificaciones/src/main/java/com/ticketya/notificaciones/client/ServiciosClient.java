package com.ticketya.notificaciones.client;

import java.util.List;
import java.util.Optional;

/** Consultas REST a otros microservicios para saber a quien notificar y a que direccion. */
public interface ServiciosClient {

    /** Datos de contacto (ms-usuarios). Vacio si el usuario no existe. */
    Optional<ContactoDto> contacto(Long usuarioId);

    /** Usuarios con entradas del evento (ms-compras). */
    List<Long> compradores(Long eventoId);

    /** Clientes con alguno de esos artistas como favorito que viven en esa ciudad (ms-recomendaciones). */
    List<Long> interesados(List<String> artistas, String ciudad);
}
