package com.ticketya.compras.service;

import com.ticketya.compras.client.*;
import com.ticketya.compras.dto.CompraRequest;
import com.ticketya.compras.dto.DatosMedioPago;
import com.ticketya.compras.model.MedioPago;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Base de los tests de ms-compras: base H2 real (para probar transacciones, restricciones
 * de unicidad, etc.) y los otros microservicios mockeados.
 */
abstract class ComprasTestBase {

    protected static final Long CLIENTE = 100L;
    protected static final DatosMedioPago TARJETA_OK =
            new DatosMedioPago(MedioPago.TARJETA_CREDITO, "4509953566233704", "Juan Perez", 1);
    protected static final DatosMedioPago TARJETA_SIN_FONDOS =
            new DatosMedioPago(MedioPago.TARJETA_CREDITO, "4000000000000002", "Juan Perez", 1);

    private static final AtomicLong SECUENCIA = new AtomicLong(1);

    @MockBean
    protected ReservasClient reservasClient;
    @MockBean
    protected PagosClient pagosClient;
    @MockBean
    protected EventosClient eventosClient;
    @Autowired
    protected CompraService compraService;

    protected long nuevoId() {
        return SECUENCIA.getAndIncrement();
    }

    /** Reserva pendiente de 2 asientos numerados (fila C, 11 y 12) a 45000 c/u. */
    protected ReservaDto reservaPendiente(long reservaId, long eventoId, LocalDateTime fechaEvento) {
        return new ReservaDto(reservaId, CLIENTE, eventoId, "Recital", fechaEvento, 3L, "Platea", 2,
                new BigDecimal("45000"), new BigDecimal("90000"), "PENDIENTE", LocalDateTime.now().plusMinutes(8),
                480, List.of(new ReservaDto.Item(eventoId * 100 + 11, "C", 11),
                new ReservaDto.Item(eventoId * 100 + 12, "C", 12)));
    }

    protected ReservaDto confirmada(ReservaDto r) {
        return new ReservaDto(r.id(), r.usuarioId(), r.eventoId(), r.nombreEvento(), r.fechaEvento(), r.sectorId(),
                r.sector(), r.cantidad(), r.precioUnitario(), r.montoTotal(), "CONFIRMADA", r.expiraEn(), 0, r.items());
    }

    /** Deja los mocks listos para que la compra de esa reserva salga aprobada. */
    protected ReservaDto prepararCompraAprobada(long eventoId, LocalDateTime fechaEvento) {
        return prepararCompraAprobada(eventoId, fechaEvento, 0);
    }

    /** Igual que el anterior pero corre los asientos (para varias compras del mismo evento). */
    protected ReservaDto prepararCompraAprobada(long eventoId, LocalDateTime fechaEvento, int corrimiento) {
        long reservaId = nuevoId();
        ReservaDto base = reservaPendiente(reservaId, eventoId, fechaEvento);
        ReservaDto reserva = corrimiento == 0 ? base : new ReservaDto(base.id(), base.usuarioId(), base.eventoId(),
                base.nombreEvento(), base.fechaEvento(), base.sectorId(), base.sector(), base.cantidad(),
                base.precioUnitario(), base.montoTotal(), base.estado(), base.expiraEn(), base.segundosRestantes(),
                base.items().stream().map(i -> new ReservaDto.Item(i.asientoId() + corrimiento, i.fila(),
                        i.numero() + corrimiento)).toList());
        when(reservasClient.obtener(reservaId)).thenReturn(reserva);
        when(reservasClient.confirmar(reservaId)).thenReturn(confirmada(reserva));
        when(pagosClient.pagar(any(), eq(CLIENTE), any(), any(), anyString()))
                .thenAnswer(inv -> new PagoDto(1000L + (Long) inv.getArgument(0), inv.getArgument(0),
                        inv.getArgument(2), "APROBADO", "AUT-TEST", null));
        return reserva;
    }

    protected ResultadoCompra comprar(long reservaId) {
        return compraService.comprar(CLIENTE, new CompraRequest(reservaId, TARJETA_OK), UUID.randomUUID().toString());
    }
}
