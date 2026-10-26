package com.odontogest.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class RelojAplicacionTestASEJ {

    private final RelojAplicacionASEJ relojASEJ = new RelojAplicacionASEJ(ZoneId.of("America/Lima"));

    @Test
    void sinFijar_daLaHoraRealASEJ() {
        assertThat(relojASEJ.esHoraRealASEJ()).isTrue();
        assertThat(Duration.between(LocalDateTime.now(ZoneId.of("America/Lima")), LocalDateTime.now(relojASEJ)).abs())
                .isLessThan(Duration.ofSeconds(2));
    }

    @Test
    void fijado_devuelveEseMomentoHastaLiberarloASEJ() {
        LocalDateTime momentoASEJ = LocalDateTime.of(2026, 5, 4, 10, 30);
        relojASEJ.fijarASEJ(momentoASEJ);
        assertThat(relojASEJ.esHoraRealASEJ()).isFalse();
        assertThat(LocalDateTime.now(relojASEJ)).isEqualTo(momentoASEJ);
        assertThat(LocalDateTime.now(relojASEJ)).as("no avanza solo").isEqualTo(momentoASEJ);
        relojASEJ.liberarASEJ();
        assertThat(relojASEJ.esHoraRealASEJ()).isTrue();
        assertThat(LocalDateTime.now(relojASEJ)).isAfter(momentoASEJ);
    }

    @Test
    void otraZonaASEJ() {
        assertThat(relojASEJ.withZone(ZoneId.of("America/Lima"))).isSameAs(relojASEJ);
        relojASEJ.fijarASEJ(LocalDateTime.of(2026, 5, 4, 10, 30));
        Clock utcASEJ = relojASEJ.withZone(ZoneId.of("UTC"));
        assertThat(LocalDateTime.now(utcASEJ)).isEqualTo(LocalDateTime.of(2026, 5, 4, 15, 30));
    }
}
