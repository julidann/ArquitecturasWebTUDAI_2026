package com.ticketya.compras.service;

import com.ticketya.compras.dto.ResumenAccesoResponse;
import com.ticketya.compras.dto.ValidacionAccesoRequest;
import com.ticketya.compras.dto.ValidacionAccesoResponse;
import com.ticketya.compras.model.Entrada;
import com.ticketya.compras.model.EstadoEntrada;
import com.ticketya.compras.model.RegistroAcceso;
import com.ticketya.compras.repository.EntradaRepository;
import com.ticketya.compras.repository.RegistroAccesoRepository;
import com.ticketya.compras.trazabilidad.Bitacora;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Control de acceso con QR (funcionalidad 8). El personal de acceso escanea el QR y el sistema valida
 * que la entrada exista, sea de ese evento y no se haya usado, y la marca como UTILIZADA.
 *
 * La marca se hace con un UPDATE condicional (estado = VALIDA) en una sola sentencia: si dos lectores
 * escanean el mismo QR a la vez (por ejemplo una captura de pantalla compartida), solo uno ve la fila
 * actualizada y el otro recibe "ya utilizada". Funciona igual con varias instancias del servicio.
 */
@Service
public class AccesoService {

    public static final String PERMITIDO = "PERMITIDO";

    private final EntradaRepository entradaRepository;
    private final RegistroAccesoRepository registroRepository;
    private final Bitacora bitacora;
    private final Clock clock;

    public AccesoService(EntradaRepository entradaRepository, RegistroAccesoRepository registroRepository,
                         Bitacora bitacora, Clock clock) {
        this.entradaRepository = entradaRepository;
        this.registroRepository = registroRepository;
        this.bitacora = bitacora;
        this.clock = clock;
    }

    @Transactional
    public ValidacionAccesoResponse validar(ValidacionAccesoRequest req, Long personalId) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        String qr = req.codigoQr().trim();
        int marcadas = entradaRepository.marcarUtilizada(qr, req.eventoId(), req.puerta(), ahora);
        Optional<Entrada> entrada = entradaRepository.findByCodigoQr(qr);

        ValidacionAccesoResponse r;
        if (marcadas == 1) {
            Entrada e = entrada.orElseThrow();
            r = new ValidacionAccesoResponse(true, PERMITIDO, "Entrada válida. ¡Bienvenido!", e.getId(),
                    e.getCompra().getSectorNombre(), e.getFila(), e.getNumero(), null);
        } else if (entrada.isEmpty()) {
            r = rechazo("INEXISTENTE", "El QR no corresponde a ninguna entrada", null);
        } else {
            Entrada e = entrada.get();
            if (!e.getEventoId().equals(req.eventoId())) {
                r = rechazo("OTRO_EVENTO", "La entrada es de otro evento", e);
            } else if (e.getEstado() == EstadoEntrada.UTILIZADA) {
                r = new ValidacionAccesoResponse(false, "YA_UTILIZADA", "La entrada ya se usó para ingresar"
                        + (e.getPuertaIngreso() == null ? "" : " por " + e.getPuertaIngreso()), e.getId(),
                        e.getCompra().getSectorNombre(), e.getFila(), e.getNumero(), e.getUtilizadaEn());
            } else {
                r = rechazo(e.getEstado().name(), "La entrada está " + e.getEstado() + " y no permite ingresar", e);
            }
        }
        registroRepository.save(new RegistroAcceso(req.eventoId(), qr, r.entradaId(), personalId, req.puerta(),
                r.resultado(), ahora));
        Map<String, Object> detalle = new HashMap<>();
        detalle.put("eventoId", req.eventoId());
        detalle.put("entradaId", r.entradaId());
        detalle.put("puerta", req.puerta());
        bitacora.registrar("acceso.validado", r.resultado(), personalId, detalle);
        return r;
    }

    @Transactional(readOnly = true)
    public ResumenAccesoResponse resumen(Long eventoId) {
        long vigentes = entradaRepository.findByEventoId(eventoId).stream()
                .filter(e -> e.getEstado() == EstadoEntrada.VALIDA || e.getEstado() == EstadoEntrada.UTILIZADA)
                .count();
        long ingresaron = registroRepository.countByEventoIdAndResultado(eventoId, PERMITIDO);
        long total = registroRepository.countByEventoId(eventoId);
        return new ResumenAccesoResponse(eventoId, vigentes, ingresaron, total - ingresaron);
    }

    private ValidacionAccesoResponse rechazo(String resultado, String mensaje, Entrada e) {
        return new ValidacionAccesoResponse(false, resultado, mensaje, e == null ? null : e.getId(), null, null, null, null);
    }
}
