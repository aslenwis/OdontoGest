package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.ArcadaASEJ;
import com.odontogest.dominio.odontograma.PiezaDentalASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import javafx.geometry.Rectangle2D;

import java.util.Optional;

public final class DetectorSuperficieASEJ {

    static final double FACTOR_CENTROASEJ = 0.2;

    private DetectorSuperficieASEJ() {
    }

    public static Optional<SuperficieASEJ> detectarASEJ(double xASEJ, double yASEJ, Rectangle2D coronaASEJ, PiezaDentalASEJ piezaASEJ) {
        double sASEJ = coronaASEJ.getWidth();
        double hASEJ = sASEJ / 2;
        double rASEJ = FACTOR_CENTROASEJ * sASEJ;
        double dxASEJ = xASEJ - (coronaASEJ.getMinX() + hASEJ);
        double dyASEJ = yASEJ - (coronaASEJ.getMinY() + hASEJ);
        double adxASEJ = Math.abs(dxASEJ);
        double adyASEJ = Math.abs(dyASEJ);

        if (adxASEJ > hASEJ || adyASEJ > hASEJ) {
            return Optional.empty();
        }
        if (adxASEJ <= rASEJ && adyASEJ <= rASEJ) {
            return Optional.of(SuperficieASEJ.O);
        }
        if (adyASEJ >= adxASEJ) {
            boolean superiorASEJ = piezaASEJ.arcadaASEJ() == ArcadaASEJ.SUPERIOR;
            boolean arribaASEJ = dyASEJ < 0;
            return Optional.of(arribaASEJ == superiorASEJ ? SuperficieASEJ.V : SuperficieASEJ.L);
        }
        boolean derechaASEJ = dxASEJ > 0;
        return Optional.of(derechaASEJ == piezaASEJ.ladoDerechoPacienteASEJ() ? SuperficieASEJ.M : SuperficieASEJ.D);
    }
}
