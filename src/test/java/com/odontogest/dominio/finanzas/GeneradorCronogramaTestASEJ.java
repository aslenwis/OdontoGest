package com.odontogest.dominio.finanzas;

import com.odontogest.dominio.comun.ReglaDominioExceptionASEJ;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneradorCronogramaTestASEJ {

    private static final LocalDate HOYASEJ = LocalDate.of(2026, 3, 2);

    private final GeneradorCronogramaASEJ generadorASEJ = new GeneradorCronogramaASEJ();

    @Test
    void repartoObligatorioDelPlanASEJ() {
        List<CuotaProgramadaASEJ> cuotasASEJ = generadorASEJ.generarASEJ(100000, 0, 3, HOYASEJ.plusDays(1), FrecuenciaASEJ.MENSUAL, HOYASEJ);
        assertThat(cuotasASEJ).extracting(CuotaProgramadaASEJ::montoCentASEJ).containsExactly(33334L, 33333L, 33333L);
        assertThat(cuotasASEJ).extracting(CuotaProgramadaASEJ::numeroASEJ).containsExactly(1, 2, 3);
    }

    @ParameterizedTest
    @CsvSource({"100000, 0, 3", "99999, 1, 7", "123457, 20000, 12", "500, 0, 500", "1, 0, 1", "100, 50, 49"})
    void laSumaEsExactaYLasCuotasDifierenComoMaximoEnUnCentimoASEJ(long totalASEJ, long inicialASEJ, int nASEJ) {
        List<CuotaProgramadaASEJ> cuotasASEJ = generadorASEJ.generarASEJ(totalASEJ, inicialASEJ, nASEJ, HOYASEJ, FrecuenciaASEJ.SEMANAL, HOYASEJ);
        assertThat(cuotasASEJ.stream().mapToLong(CuotaProgramadaASEJ::montoCentASEJ).sum()).isEqualTo(totalASEJ);
        List<Long> normalesASEJ = cuotasASEJ.stream().filter(cASEJ -> cASEJ.numeroASEJ() > 0).map(CuotaProgramadaASEJ::montoCentASEJ).toList();
        assertThat(normalesASEJ).hasSize(nASEJ);
        assertThat(normalesASEJ.stream().mapToLong(Long::longValue).max().orElseThrow()
                - normalesASEJ.stream().mapToLong(Long::longValue).min().orElseThrow()).isLessThanOrEqualTo(1);
        assertThat(normalesASEJ).isSortedAccordingTo((aASEJ, bASEJ) -> Long.compare(bASEJ, aASEJ));
    }

    @Test
    void cuotaInicialVenceHoyASEJ() {
        List<CuotaProgramadaASEJ> cuotasASEJ = generadorASEJ.generarASEJ(100000, 20000, 2, HOYASEJ.plusMonths(1), FrecuenciaASEJ.MENSUAL, HOYASEJ);
        assertThat(cuotasASEJ).containsExactly(
                new CuotaProgramadaASEJ(0, HOYASEJ, 20000),
                new CuotaProgramadaASEJ(1, LocalDate.of(2026, 4, 2), 40000),
                new CuotaProgramadaASEJ(2, LocalDate.of(2026, 5, 2), 40000));
    }

    @Test
    void inicialQueCubreElTotalASEJ() {
        assertThat(generadorASEJ.generarASEJ(5000, 5000, 0, null, null, HOYASEJ))
                .containsExactly(new CuotaProgramadaASEJ(0, HOYASEJ, 5000));
        assertThatThrownBy(() -> generadorASEJ.generarASEJ(5000, 5000, 2, HOYASEJ, FrecuenciaASEJ.MENSUAL, HOYASEJ))
                .isInstanceOf(ReglaDominioExceptionASEJ.class);
    }

    @Test
    void mensualAjustaElFinDeMesSinAcumularDesfaseASEJ() {
        LocalDate enero31ASEJ = LocalDate.of(2026, 1, 31);
        List<CuotaProgramadaASEJ> cuotasASEJ = generadorASEJ.generarASEJ(3000, 0, 3, enero31ASEJ, FrecuenciaASEJ.MENSUAL, enero31ASEJ);
        assertThat(cuotasASEJ).extracting(CuotaProgramadaASEJ::vencimientoASEJ).containsExactly(
                LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31));
    }

    @Test
    void quincenalYSemanalASEJ() {
        assertThat(generadorASEJ.generarASEJ(300, 0, 3, HOYASEJ, FrecuenciaASEJ.QUINCENAL, HOYASEJ))
                .extracting(CuotaProgramadaASEJ::vencimientoASEJ)
                .containsExactly(HOYASEJ, HOYASEJ.plusDays(15), HOYASEJ.plusDays(30));
        assertThat(generadorASEJ.generarASEJ(300, 0, 3, HOYASEJ, FrecuenciaASEJ.SEMANAL, HOYASEJ))
                .extracting(CuotaProgramadaASEJ::vencimientoASEJ)
                .containsExactly(HOYASEJ, HOYASEJ.plusWeeks(1), HOYASEJ.plusWeeks(2));
    }

    @Test
    void unVencimientoEnDomingoPasaAlLunesASEJ() {
        LocalDate domingoASEJ = LocalDate.of(2026, 3, 8);
        assertThat(domingoASEJ.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        List<CuotaProgramadaASEJ> cuotasASEJ = generadorASEJ.generarASEJ(200, 0, 2, domingoASEJ, FrecuenciaASEJ.SEMANAL, HOYASEJ);
        assertThat(cuotasASEJ).extracting(CuotaProgramadaASEJ::vencimientoASEJ)
                .containsExactly(LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 16));
    }

    @Test
    void validacionesASEJ() {
        assertThatThrownBy(() -> generadorASEJ.generarASEJ(0, 0, 1, HOYASEJ, FrecuenciaASEJ.MENSUAL, HOYASEJ))
                .isInstanceOf(ReglaDominioExceptionASEJ.class).hasMessageContaining("mayor que cero");
        assertThatThrownBy(() -> generadorASEJ.generarASEJ(1000, 1001, 1, HOYASEJ, FrecuenciaASEJ.MENSUAL, HOYASEJ))
                .isInstanceOf(ReglaDominioExceptionASEJ.class);
        assertThatThrownBy(() -> generadorASEJ.generarASEJ(1000, -1, 1, HOYASEJ, FrecuenciaASEJ.MENSUAL, HOYASEJ))
                .isInstanceOf(ReglaDominioExceptionASEJ.class);
        assertThatThrownBy(() -> generadorASEJ.generarASEJ(1000, 0, 0, HOYASEJ, FrecuenciaASEJ.MENSUAL, HOYASEJ))
                .isInstanceOf(ReglaDominioExceptionASEJ.class);
        assertThatThrownBy(() -> generadorASEJ.generarASEJ(3, 0, 4, HOYASEJ, FrecuenciaASEJ.MENSUAL, HOYASEJ))
                .isInstanceOf(ReglaDominioExceptionASEJ.class).hasMessageContaining("céntimos");
        assertThatThrownBy(() -> generadorASEJ.generarASEJ(1000, 0, 2, HOYASEJ.minusDays(1), FrecuenciaASEJ.MENSUAL, HOYASEJ))
                .isInstanceOf(ReglaDominioExceptionASEJ.class).hasMessageContaining("anterior a hoy");
    }
}
