/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Máscara de bits de superficies {@code O=1, M=2, D=4, V=8, L=16}.
 */
class SuperficiesTest {

    @Test
    void idaYVueltaParaTodasLasMascaras() {
        for (int m = 0; m <= Superficies.MASCARA_MAXIMA; m++) {
            assertThat(Superficies.aMascara(Superficies.desdeMascara(m))).isEqualTo(m);
        }
    }

    @Test
    void mesioOclusoDistalEsSiete() {
        assertThat(Superficies.aMascara(EnumSet.of(Superficie.M, Superficie.O, Superficie.D))).isEqualTo(7);
        assertThat(Superficies.desdeMascara(24)).containsExactly(Superficie.V, Superficie.L);
        assertThat(Superficies.aMascara(null)).isZero();
    }

    @Test
    void rechazaMascarasFueraDeRango() {
        assertThatThrownBy(() -> Superficies.desdeMascara(32)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Superficies.desdeMascara(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
