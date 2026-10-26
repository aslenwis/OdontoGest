package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.PiezaDentalASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeometriaCoronaTestASEJ {

    private static final Rectangle2D CORONAASEJ = new Rectangle2D(100, 50, 100, 100);

    @Test
    void elCentroDeCadaPoligonoSeDetectaComoSuSuperficieASEJ() {
        for (PiezaDentalASEJ pASEJ : PiezaDentalASEJ.filasASEJ(com.odontogest.dominio.odontograma.DenticionASEJ.MIXTA).stream()
                .flatMap(java.util.List::stream).toList()) {
            for (SuperficieASEJ sASEJ : SuperficieASEJ.values()) {
                double[] cASEJ = GeometriaCoronaASEJ.centroASEJ(GeometriaCoronaASEJ.poligonoASEJ(CORONAASEJ, pASEJ, sASEJ));
                assertThat(DetectorSuperficieASEJ.detectarASEJ(cASEJ[0], cASEJ[1], CORONAASEJ, pASEJ)).as("pieza %s, superficie %s", pASEJ, sASEJ).contains(sASEJ);
            }
        }
    }

    @Test
    void regionesSegunArcadaYLadoASEJ() {
        assertThat(GeometriaCoronaASEJ.regionASEJ(PiezaDentalASEJ.deASEJ(16), SuperficieASEJ.V)).isEqualTo(GeometriaCoronaASEJ.RegionASEJ.ARRIBA);
        assertThat(GeometriaCoronaASEJ.regionASEJ(PiezaDentalASEJ.deASEJ(46), SuperficieASEJ.V)).isEqualTo(GeometriaCoronaASEJ.RegionASEJ.ABAJO);
        assertThat(GeometriaCoronaASEJ.regionASEJ(PiezaDentalASEJ.deASEJ(16), SuperficieASEJ.M)).isEqualTo(GeometriaCoronaASEJ.RegionASEJ.DERECHA);
        assertThat(GeometriaCoronaASEJ.regionASEJ(PiezaDentalASEJ.deASEJ(26), SuperficieASEJ.M)).isEqualTo(GeometriaCoronaASEJ.RegionASEJ.IZQUIERDA);
        assertThat(GeometriaCoronaASEJ.regionASEJ(PiezaDentalASEJ.deASEJ(36), SuperficieASEJ.O)).isEqualTo(GeometriaCoronaASEJ.RegionASEJ.CENTRO);
    }

    @Test
    void elCuadradoInteriorMideCuatroDecimosDelLadoASEJ() {
        double[][] centroASEJ = GeometriaCoronaASEJ.poligonoASEJ(CORONAASEJ, GeometriaCoronaASEJ.RegionASEJ.CENTRO);
        assertThat(centroASEJ[0][1] - centroASEJ[0][0]).isCloseTo(40, within(1e-9));
        assertThat(centroASEJ[1][2] - centroASEJ[1][0]).isCloseTo(40, within(1e-9));
    }

    @Test
    void bordeExteriorDeLaSuperficieASEJ() {
        assertThat(GeometriaCoronaASEJ.bordeExteriorASEJ(CORONAASEJ, PiezaDentalASEJ.deASEJ(11), SuperficieASEJ.V)).containsExactly(100, 50, 200, 50);
        assertThat(GeometriaCoronaASEJ.bordeExteriorASEJ(CORONAASEJ, PiezaDentalASEJ.deASEJ(41), SuperficieASEJ.V)).containsExactly(200, 150, 100, 150);
        assertThat(GeometriaCoronaASEJ.bordeExteriorASEJ(CORONAASEJ, PiezaDentalASEJ.deASEJ(11), SuperficieASEJ.O)[1]).isCloseTo(100, within(1e-9));
    }
}
