package com.ticketya.bitacora.mensajeria;

import com.ticketya.bitacora.config.MensajeriaConfig;
import com.ticketya.bitacora.service.BitacoraService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class BitacoraListener {

    private final BitacoraService service;

    public BitacoraListener(BitacoraService service) {
        this.service = service;
    }

    @RabbitListener(queues = MensajeriaConfig.COLA)
    public void recibir(RegistroMensaje m) {
        service.guardar(m);
    }
}
