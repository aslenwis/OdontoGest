package com.odontogest.dominio.finanzas;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstadoCuotaTestASEJ {

    private static final LocalDate HOYASEJ = LocalDate.of(2026, 10, 20);

    @Test
    void pagadaTienePrioridadAunqueEsteVencidaASEJ() {
        assertThat(EstadoCuotaASEJ.deASEJ(10000, 10000, HOYASEJ.minusDays(30), HOYASEJ)).isEqualTo(EstadoCuotaASEJ.PAGADA);
    }

    @Test
    void vencidaSiElVencimientoPasoYFaltaPagarASEJ() {
        assertThat(EstadoCuotaASEJ.deASEJ(10000, 0, HOYASEJ.minusDays(1), HOYASEJ)).isEqualTo(EstadoCuotaASEJ.VENCIDA);
        assertThat(EstadoCuotaASEJ.deASEJ(10000, 5000, HOYASEJ.minusDays(1), HOYASEJ)).isEqualTo(EstadoCuotaASEJ.VENCIDA);
    }

    @Test
    void elDiaDelVencimientoAunNoEstaVencidaASEJ() {
        assertThat(EstadoCuotaASEJ.deASEJ(10000, 0, HOYASEJ, HOYASEJ)).isEqualTo(EstadoCuotaASEJ.PENDIENTE);
        assertThat(EstadoCuotaASEJ.deASEJ(10000, 1, HOYASEJ, HOYASEJ)).isEqualTo(EstadoCuotaASEJ.PARCIAL);
    }

    @Test
    void pagadoFueraDeRangoASEJ() {
        assertThatThrownBy(() -> EstadoCuotaASEJ.deASEJ(100, 101, HOYASEJ, HOYASEJ)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EstadoCuotaASEJ.deASEJ(100, -1, HOYASEJ, HOYASEJ)).isInstanceOf(IllegalArgumentException.class);
    }
}
