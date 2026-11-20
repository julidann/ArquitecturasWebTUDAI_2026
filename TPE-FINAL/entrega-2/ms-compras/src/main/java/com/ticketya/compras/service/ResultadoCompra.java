package com.ticketya.compras.service;

import com.ticketya.compras.dto.CompraResponse;
import org.springframework.http.HttpStatus;

/** La compra y el codigo HTTP que corresponde devolver segun como termino. */
public record ResultadoCompra(CompraResponse compra, HttpStatus status) {
}
