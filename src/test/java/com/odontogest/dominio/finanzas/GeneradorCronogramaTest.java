/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.finanzas;

import com.odontogest.dominio.comun.ReglaDominioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cronograma de cuotas con reparto exacto de céntimos.
 */
class GeneradorCronogramaTest {

    /** Lunes. */
    private static final LocalDate HOY = LocalDate.of(2026, 3, 2);

    private final GeneradorCronograma generador = new GeneradorCronograma();

    @Test
    void repartoObligatorioDelPlan() {
        List<CuotaProgramada> cuotas = generador.generar(100000, 0, 3, HOY.plusDays(1), Frecuencia.MENSUAL, HOY);
        assertThat(cuotas).extracting(CuotaProgramada::montoCent).containsExactly(33334L, 33333L, 33333L);
        assertThat(cuotas).extracting(CuotaProgramada::numero).containsExactly(1, 2, 3);
    }

    @ParameterizedTest
    @CsvSource({"100000, 0, 3", "99999, 1, 7", "123457, 20000, 12", "500, 0, 500", "1, 0, 1", "100, 50, 49"})
    void laSumaEsExactaYLasCuotasDifierenComoMaximoEnUnCentimo(long total, long inicial, int n) {
        List<CuotaProgramada> cuotas = generador.generar(total, inicial, n, HOY, Frecuencia.SEMANAL, HOY);
        assertThat(cuotas.stream().mapToLong(CuotaProgramada::montoCent).sum()).isEqualTo(total);
        List<Long> normales = cuotas.stream().filter(c -> c.numero() > 0).map(CuotaProgramada::montoCent).toList();
        assertThat(normales).hasSize(n);
        assertThat(normales.stream().mapToLong(Long::longValue).max().orElseThrow()
                - normales.stream().mapToLong(Long::longValue).min().orElseThrow()).isLessThanOrEqualTo(1);
        assertThat(normales).isSortedAccordingTo((a, b) -> Long.compare(b, a));
    }

    @Test
    void cuotaInicialVenceHoy() {
        List<CuotaProgramada> cuotas = generador.generar(100000, 20000, 2, HOY.plusMonths(1), Frecuencia.MENSUAL, HOY);
        assertThat(cuotas).containsExactly(
                new CuotaProgramada(0, HOY, 20000),
                new CuotaProgramada(1, LocalDate.of(2026, 4, 2), 40000),
                new CuotaProgramada(2, LocalDate.of(2026, 5, 2), 40000));
    }

    @Test
    void inicialQueCubreElTotal() {
        assertThat(generador.generar(5000, 5000, 0, null, null, HOY))
                .containsExactly(new CuotaProgramada(0, HOY, 5000));
        assertThatThrownBy(() -> generador.generar(5000, 5000, 2, HOY, Frecuencia.MENSUAL, HOY))
                .isInstanceOf(ReglaDominioException.class);
    }

    @Test
    void mensualAjustaElFinDeMesSinAcumularDesfase() {
        LocalDate enero31 = LocalDate.of(2026, 1, 31);
        List<CuotaProgramada> cuotas = generador.generar(3000, 0, 3, enero31, Frecuencia.MENSUAL, enero31);
        assertThat(cuotas).extracting(CuotaProgramada::vencimiento).containsExactly(
                LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31));
    }

    @Test
    void quincenalYSemanal() {
        assertThat(generador.generar(300, 0, 3, HOY, Frecuencia.QUINCENAL, HOY))
                .extracting(CuotaProgramada::vencimiento)
                .containsExactly(HOY, HOY.plusDays(15), HOY.plusDays(30));
        assertThat(generador.generar(300, 0, 3, HOY, Frecuencia.SEMANAL, HOY))
                .extracting(CuotaProgramada::vencimiento)
                .containsExactly(HOY, HOY.plusWeeks(1), HOY.plusWeeks(2));
    }

    @Test
    void unVencimientoEnDomingoPasaAlLunes() {
        LocalDate domingo = LocalDate.of(2026, 3, 8);
        assertThat(domingo.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        List<CuotaProgramada> cuotas = generador.generar(200, 0, 2, domingo, Frecuencia.SEMANAL, HOY);
        assertThat(cuotas).extracting(CuotaProgramada::vencimiento)
                .containsExactly(LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 16));
    }

    @Test
    void validaciones() {
        assertThatThrownBy(() -> generador.generar(0, 0, 1, HOY, Frecuencia.MENSUAL, HOY))
                .isInstanceOf(ReglaDominioException.class).hasMessageContaining("mayor que cero");
        assertThatThrownBy(() -> generador.generar(1000, 1001, 1, HOY, Frecuencia.MENSUAL, HOY))
                .isInstanceOf(ReglaDominioException.class);
        assertThatThrownBy(() -> generador.generar(1000, -1, 1, HOY, Frecuencia.MENSUAL, HOY))
                .isInstanceOf(ReglaDominioException.class);
        assertThatThrownBy(() -> generador.generar(1000, 0, 0, HOY, Frecuencia.MENSUAL, HOY))
                .isInstanceOf(ReglaDominioException.class);
        assertThatThrownBy(() -> generador.generar(3, 0, 4, HOY, Frecuencia.MENSUAL, HOY))
                .isInstanceOf(ReglaDominioException.class).hasMessageContaining("céntimos");
        assertThatThrownBy(() -> generador.generar(1000, 0, 2, HOY.minusDays(1), Frecuencia.MENSUAL, HOY))
                .isInstanceOf(ReglaDominioException.class).hasMessageContaining("anterior a hoy");
    }
}
