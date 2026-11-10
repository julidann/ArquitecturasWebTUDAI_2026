package com.ticketya.reservas;

import java.time.*;

/** Reloj que se puede adelantar a mano para probar vencimientos sin esperar 10 minutos. */
public class MutableClock extends Clock {

    private Instant instante = Instant.now();
    private final ZoneId zona = ZoneId.systemDefault();

    public void adelantar(Duration d) {
        instante = instante.plus(d);
    }

    public void resetear() {
        instante = Instant.now();
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
