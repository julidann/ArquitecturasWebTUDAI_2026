package com.ticketya.notificaciones.repository;

import com.ticketya.notificaciones.model.EstadoNotificacion;
import com.ticketya.notificaciones.model.Notificacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacionRepository extends MongoRepository<Notificacion, String> {

    List<Notificacion> findByUsuarioIdOrderByCreadaDesc(Long usuarioId, Pageable pagina);

    List<Notificacion> findByEstadoAndProximoIntentoLessThanEqual(EstadoNotificacion estado, LocalDateTime hasta,
                                                                  Pageable pagina);

    List<Notificacion> findByEventoId(Long eventoId);
}
