package com.ticketya.pagos.service;

import com.ticketya.pagos.dto.*;
import com.ticketya.pagos.exception.ClaveIdempotenciaReutilizadaException;
import com.ticketya.pagos.exception.OperacionInvalidaException;
import com.ticketya.pagos.exception.RecursoNoEncontradoException;
import com.ticketya.pagos.model.EstadoPago;
import com.ticketya.pagos.model.Pago;
import com.ticketya.pagos.model.Reintegro;
import com.ticketya.pagos.repository.PagoRepository;
import com.ticketya.pagos.repository.ReintegroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Cobros y reintegros. Ambas operaciones son idempotentes por Idempotency-Key: si
 * ms-compras reintenta (por ejemplo porque se corto la conexion antes de recibir la
 * respuesta) se devuelve el resultado original sin volver a cobrar.
 */
@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagoRepository;
    private final ReintegroRepository reintegroRepository;
    private final PasarelaMock pasarela;
    private final TransactionTemplate tx;

    public PagoService(PagoRepository pagoRepository, ReintegroRepository reintegroRepository,
                       PasarelaMock pasarela, TransactionTemplate tx) {
        this.pagoRepository = pagoRepository;
        this.reintegroRepository = reintegroRepository;
        this.pasarela = pasarela;
        this.tx = tx;
    }

    public ResultadoOperacion<PagoResponse> pagar(PagoRequest req, String clave) {
        validarClave(clave);
        if (pagoRepository.findByIdempotencyKey(clave).isPresent()) {
            return repetido(clave, req);
        }
        if (req.medioPago().tipo().esTarjeta() && req.medioPago().numeroTarjeta() == null) {
            throw new IllegalArgumentException("Para pagar con tarjeta hay que indicar numeroTarjeta");
        }

        PasarelaMock.Resultado resultado = pasarela.cobrar(req.monto(), req.medioPago());
        String numero = req.medioPago().numeroTarjeta();
        Pago pago = new Pago(clave, req.compraId(), req.usuarioId(), req.monto(), req.medioPago().tipo(),
                numero == null ? null : numero.substring(numero.length() - 4), req.medioPago().titular(),
                req.medioPago().cuotas());
        if (resultado.aprobado()) {
            pago.aprobar(resultado.codigoAutorizacion());
        } else {
            pago.rechazar(resultado.motivoRechazo());
        }
        try {
            Pago guardado = tx.execute(s -> pagoRepository.saveAndFlush(pago));
            log.info("Pago {} de la compra {}: {} {}", guardado.getId(), req.compraId(), guardado.getEstado(),
                    guardado.getMotivoRechazo() == null ? "" : "(" + guardado.getMotivoRechazo() + ")");
            return new ResultadoOperacion<>(PagoResponse.de(guardado), true);
        } catch (DataIntegrityViolationException e) {
            // dos pedidos con la misma clave llegaron al mismo tiempo: gana el primero que se guardo
            return repetido(clave, req);
        }
    }

    private ResultadoOperacion<PagoResponse> repetido(String clave, PagoRequest req) {
        return tx.execute(s -> {
            Pago pago = pagoRepository.findByIdempotencyKey(clave).orElseThrow();
            if (!pago.getCompraId().equals(req.compraId()) || pago.getMonto().compareTo(req.monto()) != 0) {
                throw new ClaveIdempotenciaReutilizadaException(
                        "La Idempotency-Key ya se usó para otro pago (compra " + pago.getCompraId() + ")");
            }
            log.info("Pago repetido con la misma clave, se devuelve el pago {} sin volver a cobrar", pago.getId());
            return new ResultadoOperacion<>(PagoResponse.de(pago), false);
        });
    }

    public PagoResponse obtener(Long id) {
        return tx.execute(s -> PagoResponse.de(buscar(id)));
    }

    public List<PagoResponse> deCompra(Long compraId) {
        return tx.execute(s -> pagoRepository.findByCompraIdOrderByIdDesc(compraId).stream()
                .map(PagoResponse::de).toList());
    }

    public ResultadoOperacion<ReintegroResponse> reintegrar(Long pagoId, ReintegroRequest req, String clave) {
        validarClave(clave);
        if (reintegroRepository.findByIdempotencyKey(clave).isPresent()) {
            return tx.execute(s -> new ResultadoOperacion<>(
                    ReintegroResponse.de(reintegroRepository.findByIdempotencyKey(clave).orElseThrow()), false));
        }
        try {
            return tx.execute(s -> {
                Pago pago = buscar(pagoId);
                if (pago.getEstado() != EstadoPago.APROBADO) {
                    throw new OperacionInvalidaException("Solo se pueden reintegrar pagos aprobados");
                }
                if (pago.totalReintegrado().add(req.monto()).compareTo(pago.getMonto()) > 0) {
                    throw new OperacionInvalidaException("El monto a reintegrar supera lo cobrado (cobrado "
                            + pago.getMonto() + ", ya reintegrado " + pago.totalReintegrado() + ")");
                }
                pasarela.reintegrar(req.monto());
                Reintegro r = new Reintegro(pago, clave, req.monto(), req.motivo());
                pago.getReintegros().add(r);
                reintegroRepository.saveAndFlush(r);
                log.info("Reintegro de {} sobre el pago {} ({})", req.monto(), pagoId, req.motivo());
                return new ResultadoOperacion<>(ReintegroResponse.de(r), true);
            });
        } catch (DataIntegrityViolationException e) {
            return tx.execute(s -> new ResultadoOperacion<>(
                    ReintegroResponse.de(reintegroRepository.findByIdempotencyKey(clave).orElseThrow()), false));
        }
    }

    private Pago buscar(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pago " + id));
    }

    private void validarClave(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("Falta el encabezado Idempotency-Key");
        }
    }

    /** Resultado + si se creo ahora (201) o es la respuesta de un pedido anterior (200). */
    public record ResultadoOperacion<T>(T cuerpo, boolean creado) {
    }
}
