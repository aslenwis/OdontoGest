package com.odontogest.dominio.odontograma;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.odontogest.dominio.odontograma.CatalogoPruebaASEJ.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraIndicesTestASEJ {

    private final CalculadoraIndicesASEJ calculadoraASEJ = new CalculadoraIndicesASEJ();

    @Test
    void bocaSanaTieneIndiceCeroASEJ() {
        ResultadoIndiceASEJ rASEJ = calculadoraASEJ.cpodASEJ(EstadoOdontogramaASEJ.vacioASEJ(), true);
        assertThat(rASEJ).isEqualTo(new ResultadoIndiceASEJ(0, 0, 0, 0, 28, NivelSeveridadOmsASEJ.MUY_BAJO));
        assertThat(calculadoraASEJ.cpodASEJ(EstadoOdontogramaASEJ.vacioASEJ(), false).piezasEvaluadasASEJ()).isEqualTo(32);
        assertThat(calculadoraASEJ.ceodASEJ(EstadoOdontogramaASEJ.vacioASEJ()).piezasEvaluadasASEJ()).isEqualTo(20);
    }

    @Test
    void cuentaCadaComponenteASEJ() {
        EstadoOdontogramaASEJ eASEJ = estadoASEJ(
                supASEJ(CARIESASEJ, 16, 0, SuperficieASEJ.O),
                supASEJ(CARIESASEJ, 26, 0, SuperficieASEJ.M, SuperficieASEJ.O),
                piezaASEJ(PIEZA_AUSENTEASEJ, 36, 0),
                supASEJ(RESTAURACIONASEJ, 46, 0, EstadoHallazgoASEJ.BUENO, SuperficieASEJ.O),
                piezaASEJ(CORONA_DEFINITIVAASEJ, 11, 0, EstadoHallazgoASEJ.BUENO),
                supASEJ(SELLANTEASEJ, 47, 0, EstadoHallazgoASEJ.BUENO, SuperficieASEJ.O));

        ResultadoIndiceASEJ rASEJ = calculadoraASEJ.cpodASEJ(eASEJ, true);

        assertThat(rASEJ.cASEJ()).isEqualTo(2);
        assertThat(rASEJ.pASEJ()).isEqualTo(1);
        assertThat(rASEJ.oASEJ()).isEqualTo(2);
        assertThat(rASEJ.totalASEJ()).isEqualTo(5);
        assertThat(rASEJ.nivelASEJ()).isEqualTo(NivelSeveridadOmsASEJ.ALTO);
    }

    @Test
    void precedenciaCariadaSobreObturadaASEJ() {
        EstadoOdontogramaASEJ eASEJ = estadoASEJ(
                supASEJ(RESTAURACIONASEJ, 36, 0, EstadoHallazgoASEJ.BUENO, SuperficieASEJ.O),
                supASEJ(CARIESASEJ, 36, 10, SuperficieASEJ.M));
        ResultadoIndiceASEJ rASEJ = calculadoraASEJ.cpodASEJ(eASEJ, true);
        assertThat(rASEJ.cASEJ()).isEqualTo(1);
        assertThat(rASEJ.oASEJ()).isZero();
    }

    @Test
    void restauracionEnMalEstadoOTemporalCuentaComoCariadaASEJ() {
        EstadoOdontogramaASEJ eASEJ = estadoASEJ(
                supASEJ(RESTAURACIONASEJ, 36, 0, EstadoHallazgoASEJ.MALO, SuperficieASEJ.O),
                supASEJ(RESTAURACION_TEMPORALASEJ, 46, 0, SuperficieASEJ.O),
                piezaASEJ(CORONA_TEMPORALASEJ, 11, 0));
        ResultadoIndiceASEJ rASEJ = calculadoraASEJ.cpodASEJ(eASEJ, true);
        assertThat(rASEJ.cASEJ()).isEqualTo(3);
        assertThat(rASEJ.oASEJ()).isZero();
    }

    @Test
    void cariadaSobrePerdidaYPerdidaSobreObturadaASEJ() {
        EstadoOdontogramaASEJ eASEJ = estadoASEJ(
                piezaASEJ(REMANENTE_RADICULARASEJ, 36, 0),
                piezaASEJ(IMPLANTEASEJ, 46, 0, EstadoHallazgoASEJ.BUENO),
                piezaASEJ(CORONA_DEFINITIVAASEJ, 46, 1, EstadoHallazgoASEJ.BUENO));
        ResultadoIndiceASEJ rASEJ = calculadoraASEJ.cpodASEJ(eASEJ, true);
        assertThat(rASEJ.cASEJ()).isEqualTo(1);
        assertThat(rASEJ.pASEJ()).isEqualTo(1);
        assertThat(rASEJ.oASEJ()).isZero();
    }

    @Test
    void piezaAusenteNoCuentaComoCariadaPorUnaCariesAnteriorASEJ() {
        EstadoOdontogramaASEJ eASEJ = estadoASEJ(supASEJ(CARIESASEJ, 36, 0, SuperficieASEJ.O), piezaASEJ(PIEZA_AUSENTEASEJ, 36, 60));
        ResultadoIndiceASEJ rASEJ = calculadoraASEJ.cpodASEJ(eASEJ, true);
        assertThat(rASEJ.cASEJ()).isZero();
        assertThat(rASEJ.pASEJ()).isEqualTo(1);
    }

    @Test
    void tercerasMolaresSegunConfiguracionASEJ() {
        EstadoOdontogramaASEJ eASEJ = estadoASEJ(supASEJ(CARIESASEJ, 18, 0, SuperficieASEJ.O), supASEJ(CARIESASEJ, 38, 0, SuperficieASEJ.O));
        assertThat(calculadoraASEJ.cpodASEJ(eASEJ, true).totalASEJ()).isZero();
        assertThat(calculadoraASEJ.cpodASEJ(eASEJ, false).totalASEJ()).isEqualTo(2);
    }

    @Test
    void ceodUsaPiezasDeciduasYComponenteEASEJ() {
        EstadoOdontogramaASEJ eASEJ = estadoASEJ(
                supASEJ(CARIESASEJ, 54, 0, SuperficieASEJ.O),
                piezaASEJ(PIEZA_AUSENTEASEJ, 64, 0),
                piezaASEJ(REMANENTE_RADICULARASEJ, 74, 0),
                supASEJ(RESTAURACIONASEJ, 84, 0, EstadoHallazgoASEJ.BUENO, SuperficieASEJ.O),
                supASEJ(CARIESASEJ, 36, 0, SuperficieASEJ.O));
        ResultadoIndiceASEJ rASEJ = calculadoraASEJ.ceodASEJ(eASEJ);
        assertThat(rASEJ.cASEJ()).isEqualTo(1);
        assertThat(rASEJ.pASEJ()).as("e (extraídas/indicadas)").isEqualTo(2);
        assertThat(rASEJ.oASEJ()).isEqualTo(1);
        assertThat(rASEJ.totalASEJ()).isEqualTo(4);
        assertThat(calculadoraASEJ.cpodASEJ(eASEJ, true).totalASEJ()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({
            "0, MUY_BAJO", "1.1, MUY_BAJO", "1.2, BAJO", "2.6, BAJO", "2.7, MODERADO", "4.4, MODERADO",
            "4.5, ALTO", "6.5, ALTO", "6.6, MUY_ALTO", "12, MUY_ALTO", "1.14, MUY_BAJO", "1.16, BAJO"
    })
    void nivelesOmsASEJ(double valorASEJ, NivelSeveridadOmsASEJ esperadoASEJ) {
        assertThat(NivelSeveridadOmsASEJ.deASEJ(valorASEJ)).isEqualTo(esperadoASEJ);
    }

    @Test
    void nivelOmsRechazaValoresNegativosASEJ() {
        assertThatThrownBy(() -> NivelSeveridadOmsASEJ.deASEJ(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
