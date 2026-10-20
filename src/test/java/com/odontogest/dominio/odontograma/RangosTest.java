/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import org.junit.jupiter.api.Test;

import static com.odontogest.dominio.odontograma.CatalogoPrueba.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hallazgos de rango que cubren piezas (edéntulo total, prótesis) y su efecto en índices y superficies.
 */
class RangosTest {

    private final CalculadoraIndices calculadora = new CalculadoraIndices();

    @Test
    void rangoDePiezasEnLaFilaDeDibujo() {
        assertThat(PiezaDental.rango(PiezaDental.de(18), PiezaDental.de(28))).hasSize(16)
                .startsWith(PiezaDental.de(18)).endsWith(PiezaDental.de(28));
        assertThat(PiezaDental.rango(PiezaDental.de(26), PiezaDental.de(24)))
                .extracting(PiezaDental::fdi).containsExactly(24, 25, 26);
        assertThat(PiezaDental.rango(PiezaDental.de(13), PiezaDental.de(23)))
                .extracting(PiezaDental::fdi).containsExactly(13, 12, 11, 21, 22, 23);
        assertThat(PiezaDental.rango(PiezaDental.de(55), PiezaDental.de(65))).hasSize(10);
        assertThat(PiezaDental.rango(PiezaDental.de(16), PiezaDental.de(55)))
                .as("filas distintas: solo los extremos").extracting(PiezaDental::fdi).containsExactly(16, 55);
        assertThat(PiezaDental.rango(PiezaDental.de(36), PiezaDental.de(36))).extracting(PiezaDental::fdi).containsExactly(36);
    }

    @Test
    void edentuloTotalCuentaComoPerdidaEnCadaPiezaCubierta() {
        EstadoOdontograma e = estado(rango(EDENTULO_TOTAL, 18, 28, 0, null));

        ResultadoIndice r = calculadora.cpod(e, true);
        assertThat(r.p()).as("14 piezas superiores sin terceras molares").isEqualTo(14);
        assertThat(calculadora.cpod(e, false).p()).isEqualTo(16);
        assertThat(e.rangosQueCubren(14)).hasSize(1);
        assertThat(e.rangosQueCubren(36)).isEmpty();
        assertThat(e.superficiesOcultas(14)).isTrue();
        assertThat(e.superficiesOcultas(36)).isFalse();
    }

    @Test
    void cariesPreviaBajoEdentuloNoCuentaComoCariada() {
        EstadoOdontograma e = estado(sup(CARIES, 16, 0, Superficie.O), rango(EDENTULO_TOTAL, 18, 28, 60, null));

        assertThat(e.hallazgosVisibles(16)).extracting(h -> h.tipo().codigo()).containsExactly("EDENTULO_TOTAL");
        assertThat(e.pieza(16).orElseThrow().hallazgos()).as("la caries sigue en el estado de la pieza").hasSize(1);
        ResultadoIndice r = calculadora.cpod(e, true);
        assertThat(r.c()).isZero();
        assertThat(r.p()).isEqualTo(14);
    }

    @Test
    void protesisFijaNoOcultaNiCuentaPeroSeVeEnLasPiezasCubiertas() {
        EstadoOdontograma e = estado(rango(PROTESIS_FIJA, 14, 16, 0, EstadoHallazgo.BUENO));

        assertThat(e.hallazgosVisibles(15)).extracting(h -> h.tipo().codigo()).containsExactly("PROTESIS_FIJA");
        assertThat(e.superficiesOcultas(15)).isFalse();
        assertThat(calculadora.cpod(e, true).total()).isZero();
    }

    @Test
    void piezaNoErupcionadaNoCuentaEnElCpod() {
        EstadoOdontograma e = estado(pieza(PIEZA_NO_ERUPCIONADA, 18, 0), pieza(PIEZA_AUSENTE, 36, 0));
        ResultadoIndice r = calculadora.cpod(e, false);
        assertThat(r.p()).as("solo la ausente por caries (DEX)").isEqualTo(1);
        assertThat(e.superficiesOcultas(18)).isTrue();
    }
}
