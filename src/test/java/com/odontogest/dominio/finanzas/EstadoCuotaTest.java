/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.finanzas;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Estado derivado de una cuota (PLAN §5.2.4).
 */
class EstadoCuotaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 20);

    @Test
    void pagadaTienePrioridadAunqueEsteVencida() {
        assertThat(EstadoCuota.de(10000, 10000, HOY.minusDays(30), HOY)).isEqualTo(EstadoCuota.PAGADA);
    }

    @Test
    void vencidaSiElVencimientoPasoYFaltaPagar() {
        assertThat(EstadoCuota.de(10000, 0, HOY.minusDays(1), HOY)).isEqualTo(EstadoCuota.VENCIDA);
        assertThat(EstadoCuota.de(10000, 5000, HOY.minusDays(1), HOY)).isEqualTo(EstadoCuota.VENCIDA);
    }

    @Test
    void elDiaDelVencimientoAunNoEstaVencida() {
        assertThat(EstadoCuota.de(10000, 0, HOY, HOY)).isEqualTo(EstadoCuota.PENDIENTE);
        assertThat(EstadoCuota.de(10000, 1, HOY, HOY)).isEqualTo(EstadoCuota.PARCIAL);
    }

    @Test
    void pagadoFueraDeRango() {
        assertThatThrownBy(() -> EstadoCuota.de(100, 101, HOY, HOY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EstadoCuota.de(100, -1, HOY, HOY)).isInstanceOf(IllegalArgumentException.class);
    }
}
