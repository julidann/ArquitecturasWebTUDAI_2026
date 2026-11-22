package com.ticketya.fila;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FilaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FilaApplication.class, args);
    }
}
