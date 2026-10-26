package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.DenticionASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class LayoutOdontogramaTestASEJ {

    private static final double ANCHOASEJ = 1200;
    private static final double ALTOASEJ = 700;

    @Test
    void cantidadDeCeldasPorDenticionASEJ() {
        assertThat(LayoutOdontogramaASEJ.calcularASEJ(ANCHOASEJ, ALTOASEJ, DenticionASEJ.PERMANENTE).celdasASEJ()).hasSize(32);
        assertThat(LayoutOdontogramaASEJ.calcularASEJ(ANCHOASEJ, ALTOASEJ, DenticionASEJ.DECIDUA).celdasASEJ()).hasSize(20);
        assertThat(LayoutOdontogramaASEJ.calcularASEJ(ANCHOASEJ, ALTOASEJ, DenticionASEJ.MIXTA).celdasASEJ()).hasSize(52);
    }

    @Test
    void ladoSegunLaFormulaDelPlanASEJ() {
        LayoutOdontogramaASEJ anchoASEJ = LayoutOdontogramaASEJ.calcularASEJ(ANCHOASEJ, 2000, DenticionASEJ.PERMANENTE);
        double sASEJ = anchoASEJ.ladoASEJ();
        assertThat(16 * sASEJ + 15 * 0.12 * sASEJ + 0.5 * sASEJ).as("limitado por el ancho")
                .isCloseTo(ANCHOASEJ - 2 * LayoutOdontogramaASEJ.MARGENASEJ, within(1e-6));

        LayoutOdontogramaASEJ altoASEJ = LayoutOdontogramaASEJ.calcularASEJ(3000, 400, DenticionASEJ.MIXTA);
        assertThat(altoASEJ.ladoASEJ()).as("limitado por el alto de fila")
                .isCloseTo((400 - 2 * LayoutOdontogramaASEJ.MARGENASEJ) / 4 * LayoutOdontogramaASEJ.FACTOR_ALTO_FILAASEJ, within(1e-6));
        assertThat(altoASEJ.separacionASEJ()).isCloseTo(0.12 * altoASEJ.ladoASEJ(), within(1e-9));
    }

    @ParameterizedTest
    @EnumSource(DenticionASEJ.class)
    void lasCeldasNoSeSuperponenYQuedanDentroDelAreaASEJ(DenticionASEJ denticionASEJ) {
        for (double[] areaASEJ : new double[][]{{ANCHOASEJ, ALTOASEJ}, {800, 900}, {1600, 300}}) {
            LayoutOdontogramaASEJ layoutASEJ = LayoutOdontogramaASEJ.calcularASEJ(areaASEJ[0], areaASEJ[1], denticionASEJ);
            List<Rectangle2D> rectsASEJ = new ArrayList<>();
            for (CeldaPiezaASEJ cASEJ : layoutASEJ.celdasASEJ()) {
                Rectangle2D rASEJ = cASEJ.limitesASEJ();
                assertThat(rASEJ.getMinX()).isGreaterThanOrEqualTo(0);
                assertThat(rASEJ.getMinY()).isGreaterThanOrEqualTo(0);
                assertThat(rASEJ.getMaxX()).isLessThanOrEqualTo(areaASEJ[0]);
                assertThat(rASEJ.getMaxY()).isLessThanOrEqualTo(areaASEJ[1]);
                rectsASEJ.add(rASEJ);
            }
            for (int iASEJ = 0; iASEJ < rectsASEJ.size(); iASEJ++) {
                for (int jASEJ = iASEJ + 1; jASEJ < rectsASEJ.size(); jASEJ++) {
                    assertThat(rectsASEJ.get(iASEJ).intersects(rectsASEJ.get(jASEJ))).as("celdas %d y %d", iASEJ, jASEJ).isFalse();
                }
            }
        }
    }

    @Test
    void lineaMediaCentradaYPiezasEnOrdenDeDibujoASEJ() {
        LayoutOdontogramaASEJ lASEJ = LayoutOdontogramaASEJ.calcularASEJ(ANCHOASEJ, ALTOASEJ, DenticionASEJ.MIXTA);
        double centroASEJ = ANCHOASEJ / 2;
        assertThat(lASEJ.celdaASEJ(11).coronaASEJ().getMaxX()).isLessThan(centroASEJ);
        assertThat(lASEJ.celdaASEJ(21).coronaASEJ().getMinX()).isGreaterThan(centroASEJ);
        assertThat(centroASEJ - lASEJ.celdaASEJ(11).coronaASEJ().getMaxX())
                .isCloseTo(lASEJ.celdaASEJ(21).coronaASEJ().getMinX() - centroASEJ, within(1e-9));
        assertThat(centroASEJ - lASEJ.celdaASEJ(51).coronaASEJ().getMaxX())
                .as("deciduas sobre la misma línea media").isCloseTo(centroASEJ - lASEJ.celdaASEJ(11).coronaASEJ().getMaxX(), within(1e-9));
        assertThat(lASEJ.celdaASEJ(18).coronaASEJ().getMinX()).isLessThan(lASEJ.celdaASEJ(17).coronaASEJ().getMinX());
        assertThat(lASEJ.celdaASEJ(48).coronaASEJ().getMinX()).isLessThan(lASEJ.celdaASEJ(41).coronaASEJ().getMinX());
        assertThat(lASEJ.celdaASEJ(31).coronaASEJ().getMinX()).isLessThan(lASEJ.celdaASEJ(38).coronaASEJ().getMinX());
        assertThat(lASEJ.celdaASEJ(55).coronaASEJ().getMinY()).isGreaterThan(lASEJ.celdaASEJ(18).coronaASEJ().getMinY());
        assertThat(lASEJ.celdaASEJ(48).coronaASEJ().getMinY()).isGreaterThan(lASEJ.celdaASEJ(85).coronaASEJ().getMinY());
    }

    @Test
    void ordenDeLaNtsYArcadaInferiorEnEspejoASEJ() {
        LayoutOdontogramaASEJ lASEJ = LayoutOdontogramaASEJ.calcularASEJ(ANCHOASEJ, ALTOASEJ, DenticionASEJ.PERMANENTE);
        CeldaPiezaASEJ supASEJ = lASEJ.celdaASEJ(16);
        assertThat(supASEJ.etiquetaASEJ().getY()).as("número entre recuadro y raíz")
                .isGreaterThan(supASEJ.recuadroASEJ().getMaxY()).isLessThan(supASEJ.raizASEJ().getMinY());
        assertThat(supASEJ.raizASEJ().getMaxY()).isCloseTo(supASEJ.coronaASEJ().getMinY(), within(1e-9));

        CeldaPiezaASEJ infASEJ = lASEJ.celdaASEJ(46);
        assertThat(infASEJ.coronaASEJ().getMaxY()).isCloseTo(infASEJ.raizASEJ().getMinY(), within(1e-9));
        assertThat(infASEJ.etiquetaASEJ().getY()).isGreaterThan(infASEJ.raizASEJ().getMaxY()).isLessThan(infASEJ.recuadroASEJ().getMinY());
        assertThat(infASEJ.coronaASEJ().getWidth()).isEqualTo(infASEJ.coronaASEJ().getHeight()).isEqualTo(lASEJ.ladoASEJ());

        assertThat(infASEJ.coronaASEJ().getMinY() - supASEJ.coronaASEJ().getMaxY())
                .as("margen oclusal libre entre las coronas de ambas arcadas").isGreaterThan(0.7 * lASEJ.ladoASEJ());
    }

    @Test
    void celdaEnUnPuntoYDeteccionDeSuperficieIntegradasASEJ() {
        LayoutOdontogramaASEJ lASEJ = LayoutOdontogramaASEJ.calcularASEJ(ANCHOASEJ, ALTOASEJ, DenticionASEJ.PERMANENTE);
        Rectangle2D coronaASEJ = lASEJ.celdaASEJ(36).coronaASEJ();
        double xASEJ = coronaASEJ.getMinX() + 2;
        double yASEJ = coronaASEJ.getMinY() + coronaASEJ.getHeight() / 2;

        CeldaPiezaASEJ celdaASEJ = lASEJ.celdaEnASEJ(xASEJ, yASEJ).orElseThrow();

        assertThat(celdaASEJ.piezaASEJ().fdiASEJ()).isEqualTo(36);
        assertThat(DetectorSuperficieASEJ.detectarASEJ(xASEJ, yASEJ, celdaASEJ.coronaASEJ(), celdaASEJ.piezaASEJ()))
                .as("36 se dibuja a la derecha: su lado izquierdo es mesial").contains(SuperficieASEJ.M);
        assertThat(lASEJ.celdaEnASEJ(1, 1)).isEmpty();
        assertThat(lASEJ.celdaEnASEJ(lASEJ.celdaASEJ(36).recuadroASEJ().getMinX() + 1, lASEJ.celdaASEJ(36).recuadroASEJ().getMinY() + 1))
                .map(cASEJ -> cASEJ.piezaASEJ().fdiASEJ()).contains(36);
    }

    @Test
    void piezaAjenaALaDenticionASEJ() {
        LayoutOdontogramaASEJ lASEJ = LayoutOdontogramaASEJ.calcularASEJ(ANCHOASEJ, ALTOASEJ, DenticionASEJ.PERMANENTE);
        assertThat(lASEJ.contieneASEJ(55)).isFalse();
        assertThat(lASEJ.contieneASEJ(11)).isTrue();
        assertThatThrownBy(() -> lASEJ.celdaASEJ(55)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void areaSinTamanoNoFallaASEJ() {
        LayoutOdontogramaASEJ lASEJ = LayoutOdontogramaASEJ.calcularASEJ(0, 0, DenticionASEJ.MIXTA);
        assertThat(lASEJ.ladoASEJ()).isPositive();
        assertThat(lASEJ.celdasASEJ()).hasSize(52);
    }
}
