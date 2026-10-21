/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reloj de la aplicación: hora real salvo cuando el seeder (o una prueba) lo fija.
 */
class RelojAplicacionTest {

    private final RelojAplicacion reloj = new RelojAplicacion(ZoneId.of("America/Lima"));

    @Test
    void sinFijar_daLaHoraReal() {
        assertThat(reloj.esHoraReal()).isTrue();
        assertThat(Duration.between(LocalDateTime.now(ZoneId.of("America/Lima")), LocalDateTime.now(reloj)).abs())
                .isLessThan(Duration.ofSeconds(2));
    }

    @Test
    void fijado_devuelveEseMomentoHastaLiberarlo() {
        LocalDateTime momento = LocalDateTime.of(2026, 5, 4, 10, 30);
        reloj.fijar(momento);
        assertThat(reloj.esHoraReal()).isFalse();
        assertThat(LocalDateTime.now(reloj)).isEqualTo(momento);
        assertThat(LocalDateTime.now(reloj)).as("no avanza solo").isEqualTo(momento);
        reloj.liberar();
        assertThat(reloj.esHoraReal()).isTrue();
        assertThat(LocalDateTime.now(reloj)).isAfter(momento);
    }

    @Test
    void otraZona() {
        assertThat(reloj.withZone(ZoneId.of("America/Lima"))).isSameAs(reloj);
        reloj.fijar(LocalDateTime.of(2026, 5, 4, 10, 30));
        Clock utc = reloj.withZone(ZoneId.of("UTC"));
        assertThat(LocalDateTime.now(utc)).isEqualTo(LocalDateTime.of(2026, 5, 4, 15, 30));
    }
}
