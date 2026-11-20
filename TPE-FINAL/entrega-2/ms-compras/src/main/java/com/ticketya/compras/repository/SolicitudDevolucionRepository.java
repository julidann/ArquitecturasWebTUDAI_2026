package com.ticketya.compras.repository;

import com.ticketya.compras.model.EstadoSolicitud;
import com.ticketya.compras.model.SolicitudDevolucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudDevolucionRepository extends JpaRepository<SolicitudDevolucion, Long> {

    boolean existsByEntradaIdAndEstado(Long entradaId, EstadoSolicitud estado);

    List<SolicitudDevolucion> findByEstadoOrderBySolicitadaEn(EstadoSolicitud estado);

    List<SolicitudDevolucion> findAllByOrderBySolicitadaEnDesc();

    List<SolicitudDevolucion> findByUsuarioIdOrderBySolicitadaEnDesc(Long usuarioId);

    List<SolicitudDevolucion> findByEntradaEventoIdAndEstado(Long eventoId, EstadoSolicitud estado);
}
