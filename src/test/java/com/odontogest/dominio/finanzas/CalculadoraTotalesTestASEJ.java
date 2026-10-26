package com.odontogest.dominio.finanzas;

import com.odontogest.dominio.comun.ReglaDominioExceptionASEJ;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraTotalesTestASEJ {

    private final CalculadoraTotalesASEJ calculadoraASEJ = new CalculadoraTotalesASEJ();

    @Test
    void totalDeItemConDescuentoASEJ() {
        assertThat(calculadoraASEJ.totalItemASEJ(2, 12000, 4000)).isEqualTo(20000);
        assertThat(calculadoraASEJ.totalItemASEJ(1, 12000, 12000)).isZero();
    }

    @Test
    void subtotalExcluyeCanceladosYAplicaDescuentoGlobalASEJ() {
        CalculadoraTotalesASEJ.TotalesASEJ tASEJ = calculadoraASEJ.calcularASEJ(List.of(
                new CalculadoraTotalesASEJ.LineaASEJ(1, 5000, 0, false),
                new CalculadoraTotalesASEJ.LineaASEJ(3, 12000, 1000, false),
                new CalculadoraTotalesASEJ.LineaASEJ(1, 90000, 0, true)), 2000);
        assertThat(tASEJ).isEqualTo(new CalculadoraTotalesASEJ.TotalesASEJ(40000, 2000, 38000));
        assertThat(tASEJ.totalCentASEJ()).isEqualTo(tASEJ.subtotalCentASEJ() - tASEJ.descuentoCentASEJ());
    }

    @Test
    void planVacioASEJ() {
        assertThat(calculadoraASEJ.calcularASEJ(List.of(), 0)).isEqualTo(new CalculadoraTotalesASEJ.TotalesASEJ(0, 0, 0));
    }

    @Test
    void validacionesASEJ() {
        assertThatThrownBy(() -> calculadoraASEJ.totalItemASEJ(0, 100, 0)).isInstanceOf(ReglaDominioExceptionASEJ.class);
        assertThatThrownBy(() -> calculadoraASEJ.totalItemASEJ(1, -1, 0)).isInstanceOf(ReglaDominioExceptionASEJ.class);
        assertThatThrownBy(() -> calculadoraASEJ.totalItemASEJ(1, 100, 101)).isInstanceOf(ReglaDominioExceptionASEJ.class);
        assertThatThrownBy(() -> calculadoraASEJ.totalItemASEJ(1, 100, -1)).isInstanceOf(ReglaDominioExceptionASEJ.class);
        assertThatThrownBy(() -> calculadoraASEJ.calcularASEJ(List.of(new CalculadoraTotalesASEJ.LineaASEJ(1, 100, 0, false)), 101))
                .isInstanceOf(ReglaDominioExceptionASEJ.class).hasMessageContaining("descuento global");
        assertThatThrownBy(() -> calculadoraASEJ.totalItemASEJ(Integer.MAX_VALUE, Long.MAX_VALUE / 2, 0))
                .isInstanceOf(ArithmeticException.class);
    }
}
