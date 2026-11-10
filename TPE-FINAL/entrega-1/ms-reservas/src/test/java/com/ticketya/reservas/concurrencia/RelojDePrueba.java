package com.ticketya.reservas.concurrencia;

import com.ticketya.reservas.MutableClock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class RelojDePrueba {

    @Bean
    @Primary
    MutableClock mutableClock() {
        return new MutableClock();
    }
}
