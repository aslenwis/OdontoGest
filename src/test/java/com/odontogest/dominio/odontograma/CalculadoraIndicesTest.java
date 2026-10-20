/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.odontogest.dominio.odontograma.CatalogoPrueba.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CPO-D y ceo-d con precedencia C &gt; P &gt; O y clasificación OMS.
 */
class CalculadoraIndicesTest {

    private final CalculadoraIndices calculadora = new CalculadoraIndices();

    @Test
    void bocaSanaTieneIndiceCero() {
        ResultadoIndice r = calculadora.cpod(EstadoOdontograma.vacio(), true);
        assertThat(r).isEqualTo(new ResultadoIndice(0, 0, 0, 0, 28, NivelSeveridadOms.MUY_BAJO));
        assertThat(calculadora.cpod(EstadoOdontograma.vacio(), false).piezasEvaluadas()).isEqualTo(32);
        assertThat(calculadora.ceod(EstadoOdontograma.vacio()).piezasEvaluadas()).isEqualTo(20);
    }

    @Test
    void cuentaCadaComponente() {
        EstadoOdontograma e = estado(
                sup(CARIES, 16, 0, Superficie.O),
                sup(CARIES, 26, 0, Superficie.M, Superficie.O),                     // una pieza, aunque 2 superficies
                pieza(PIEZA_AUSENTE, 36, 0),
                sup(RESTAURACION, 46, 0, EstadoHallazgo.BUENO, Superficie.O),
                pieza(CORONA_DEFINITIVA, 11, 0, EstadoHallazgo.BUENO),
                sup(SELLANTE, 47, 0, EstadoHallazgo.BUENO, Superficie.O));         // no cuenta

        ResultadoIndice r = calculadora.cpod(e, true);

        assertThat(r.c()).isEqualTo(2);
        assertThat(r.p()).isEqualTo(1);
        assertThat(r.o()).isEqualTo(2);
        assertThat(r.total()).isEqualTo(5);
        assertThat(r.nivel()).isEqualTo(NivelSeveridadOms.ALTO);
    }

    @Test
    void precedenciaCariadaSobreObturada() {
        EstadoOdontograma e = estado(
                sup(RESTAURACION, 36, 0, EstadoHallazgo.BUENO, Superficie.O),
                sup(CARIES, 36, 10, Superficie.M));
        ResultadoIndice r = calculadora.cpod(e, true);
        assertThat(r.c()).isEqualTo(1);
        assertThat(r.o()).isZero();
    }

    @Test
    void restauracionEnMalEstadoOTemporalCuentaComoCariada() {
        EstadoOdontograma e = estado(
                sup(RESTAURACION, 36, 0, EstadoHallazgo.MALO, Superficie.O),
                sup(RESTAURACION_TEMPORAL, 46, 0, Superficie.O),
                pieza(CORONA_TEMPORAL, 11, 0));
        ResultadoIndice r = calculadora.cpod(e, true);
        assertThat(r.c()).isEqualTo(3);
        assertThat(r.o()).isZero();
    }

    @Test
    void cariadaSobrePerdidaYPerdidaSobreObturada() {
        EstadoOdontograma e = estado(
                pieza(REMANENTE_RADICULAR, 36, 0),                                  // C (capa ESTADO_PIEZA)
                pieza(IMPLANTE, 46, 0, EstadoHallazgo.BUENO),                        // P
                pieza(CORONA_DEFINITIVA, 46, 1, EstadoHallazgo.BUENO));              // O: pierde ante P
        ResultadoIndice r = calculadora.cpod(e, true);
        assertThat(r.c()).isEqualTo(1);
        assertThat(r.p()).isEqualTo(1);
        assertThat(r.o()).isZero();
    }

    @Test
    void piezaAusenteNoCuentaComoCariadaPorUnaCariesAnterior() {
        EstadoOdontograma e = estado(sup(CARIES, 36, 0, Superficie.O), pieza(PIEZA_AUSENTE, 36, 60));
        ResultadoIndice r = calculadora.cpod(e, true);
        assertThat(r.c()).isZero();
        assertThat(r.p()).isEqualTo(1);
    }

    @Test
    void tercerasMolaresSegunConfiguracion() {
        EstadoOdontograma e = estado(sup(CARIES, 18, 0, Superficie.O), sup(CARIES, 38, 0, Superficie.O));
        assertThat(calculadora.cpod(e, true).total()).isZero();
        assertThat(calculadora.cpod(e, false).total()).isEqualTo(2);
    }

    @Test
    void ceodUsaPiezasDeciduasYComponenteE() {
        EstadoOdontograma e = estado(
                sup(CARIES, 54, 0, Superficie.O),
                pieza(PIEZA_AUSENTE, 64, 0),
                pieza(REMANENTE_RADICULAR, 74, 0),                                   // ceo-d: e
                sup(RESTAURACION, 84, 0, EstadoHallazgo.BUENO, Superficie.O),
                sup(CARIES, 36, 0, Superficie.O));                                   // permanente: no cuenta
        ResultadoIndice r = calculadora.ceod(e);
        assertThat(r.c()).isEqualTo(1);
        assertThat(r.p()).as("e (extraídas/indicadas)").isEqualTo(2);
        assertThat(r.o()).isEqualTo(1);
        assertThat(r.total()).isEqualTo(4);
        assertThat(calculadora.cpod(e, true).total()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({
            "0, MUY_BAJO", "1.1, MUY_BAJO", "1.2, BAJO", "2.6, BAJO", "2.7, MODERADO", "4.4, MODERADO",
            "4.5, ALTO", "6.5, ALTO", "6.6, MUY_ALTO", "12, MUY_ALTO", "1.14, MUY_BAJO", "1.16, BAJO"
    })
    void nivelesOms(double valor, NivelSeveridadOms esperado) {
        assertThat(NivelSeveridadOms.de(valor)).isEqualTo(esperado);
    }

    @Test
    void nivelOmsRechazaValoresNegativos() {
        assertThatThrownBy(() -> NivelSeveridadOms.de(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
