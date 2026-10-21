/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Transiciones de estado del plan de tratamiento (RF-PLN-04).
 */
class EstadoPlanTest {

    @Test
    void caminoNormal() {
        assertThat(EstadoPlan.BORRADOR.puedePasarA(EstadoPlan.PROPUESTO)).isTrue();
        assertThat(EstadoPlan.PROPUESTO.puedePasarA(EstadoPlan.ACEPTADO)).isTrue();
        assertThat(EstadoPlan.ACEPTADO.puedePasarA(EstadoPlan.EN_CURSO)).isTrue();
        assertThat(EstadoPlan.EN_CURSO.puedePasarA(EstadoPlan.FINALIZADO)).isTrue();
    }

    @Test
    void noSeSaltanEstados() {
        assertThat(EstadoPlan.BORRADOR.puedePasarA(EstadoPlan.ACEPTADO)).isFalse();
        assertThat(EstadoPlan.PROPUESTO.puedePasarA(EstadoPlan.EN_CURSO)).isFalse();
        assertThat(EstadoPlan.ACEPTADO.puedePasarA(EstadoPlan.FINALIZADO)).isFalse();
        assertThat(EstadoPlan.ACEPTADO.puedePasarA(EstadoPlan.BORRADOR)).as("aceptado ya no vuelve a borrador").isFalse();
    }

    @Test
    void finalizadoYAnuladoSonFinales() {
        for (EstadoPlan e : EstadoPlan.values()) {
            assertThat(EstadoPlan.FINALIZADO.puedePasarA(e)).isFalse();
            assertThat(EstadoPlan.ANULADO.puedePasarA(e)).isFalse();
        }
        assertThat(EnumSet.complementOf(EnumSet.of(EstadoPlan.FINALIZADO, EstadoPlan.ANULADO)))
                .allMatch(e -> e.puedePasarA(EstadoPlan.ANULADO));
    }

    @Test
    void capacidadesPorEstado() {
        assertThat(EnumSet.allOf(EstadoPlan.class).stream().filter(EstadoPlan::editable)).containsExactly(EstadoPlan.BORRADOR);
        assertThat(EnumSet.allOf(EstadoPlan.class).stream().filter(EstadoPlan::admiteCronograma))
                .containsExactly(EstadoPlan.PROPUESTO, EstadoPlan.ACEPTADO, EstadoPlan.EN_CURSO);
        assertThat(EnumSet.allOf(EstadoPlan.class).stream().filter(EstadoPlan::enEjecucion))
                .containsExactly(EstadoPlan.ACEPTADO, EstadoPlan.EN_CURSO);
    }
}
