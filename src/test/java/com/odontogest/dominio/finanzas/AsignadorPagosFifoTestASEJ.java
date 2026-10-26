package com.odontogest.dominio.finanzas;

import com.odontogest.dominio.comun.ReglaDominioExceptionASEJ;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsignadorPagosFifoTestASEJ {

    private static final LocalDate ENEROASEJ = LocalDate.of(2026, 1, 15);
    private static final LocalDate FEBREROASEJ = LocalDate.of(2026, 2, 15);
    private static final LocalDate MARZOASEJ = LocalDate.of(2026, 3, 15);

    private final AsignadorPagosFifoASEJ asignadorASEJ = new AsignadorPagosFifoASEJ();

    private final List<SaldoCuotaASEJ> pendientesASEJ = List.of(
            new SaldoCuotaASEJ(30, MARZOASEJ, 3, 10000),
            new SaldoCuotaASEJ(10, ENEROASEJ, 1, 4000),
            new SaldoCuotaASEJ(20, FEBREROASEJ, 2, 10000));

    @Test
    void pagaPrimeroLaCuotaMasAntiguaASEJ() {
        assertThat(asignadorASEJ.asignarASEJ(3000, pendientesASEJ)).containsExactly(new AplicacionASEJ(10, 3000));
    }

    @Test
    void unPagoGrandeCubreVariasCuotasEnOrdenASEJ() {
        assertThat(asignadorASEJ.asignarASEJ(19000, pendientesASEJ)).containsExactly(
                new AplicacionASEJ(10, 4000), new AplicacionASEJ(20, 10000), new AplicacionASEJ(30, 5000));
    }

    @Test
    void pagoExactoDelSaldoTotalASEJ() {
        List<AplicacionASEJ> aplicacionesASEJ = asignadorASEJ.asignarASEJ(24000, pendientesASEJ);
        assertThat(aplicacionesASEJ).hasSize(3);
        assertThat(aplicacionesASEJ.stream().mapToLong(AplicacionASEJ::montoCentASEJ).sum()).isEqualTo(24000);
    }

    @Test
    void aIgualVencimientoVaPrimeroElMenorNumeroASEJ() {
        List<SaldoCuotaASEJ> mismoDiaASEJ = List.of(new SaldoCuotaASEJ(2, ENEROASEJ, 1, 500), new SaldoCuotaASEJ(1, ENEROASEJ, 0, 500));
        assertThat(asignadorASEJ.asignarASEJ(700, mismoDiaASEJ)).containsExactly(new AplicacionASEJ(1, 500), new AplicacionASEJ(2, 200));
    }

    @Test
    void ignoraCuotasSinSaldoASEJ() {
        List<SaldoCuotaASEJ> conPagadaASEJ = List.of(new SaldoCuotaASEJ(1, ENEROASEJ, 1, 0), new SaldoCuotaASEJ(2, FEBREROASEJ, 2, 100));
        assertThat(asignadorASEJ.asignarASEJ(100, conPagadaASEJ)).containsExactly(new AplicacionASEJ(2, 100));
    }

    @Test
    void rechazaMontoMayorQueElSaldoASEJ() {
        assertThatThrownBy(() -> asignadorASEJ.asignarASEJ(24050, pendientesASEJ))
                .isInstanceOf(ReglaDominioExceptionASEJ.class)
                .hasMessage("El monto excede el saldo pendiente en S/ 0.50.");
        assertThatThrownBy(() -> asignadorASEJ.asignarASEJ(100, List.of()))
                .hasMessageContaining("S/ 1.00");
    }

    @Test
    void rechazaMontoNoPositivoASEJ() {
        assertThatThrownBy(() -> asignadorASEJ.asignarASEJ(0, pendientesASEJ)).isInstanceOf(ReglaDominioExceptionASEJ.class);
        assertThatThrownBy(() -> asignadorASEJ.asignarASEJ(-5, pendientesASEJ)).isInstanceOf(ReglaDominioExceptionASEJ.class);
    }
}
