package com.ticketya.reservas.concurrencia;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Prueba de no doble venta sobre H2 en memoria (corre siempre, no necesita Docker). */
@SpringBootTest
@Import(RelojDePrueba.class)
class NoDobleVentaH2Test extends NoDobleVentaTestBase {
}
