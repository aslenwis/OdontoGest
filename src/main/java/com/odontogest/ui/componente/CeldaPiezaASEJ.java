package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.PiezaDentalASEJ;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

public record CeldaPiezaASEJ(PiezaDentalASEJ piezaASEJ, Rectangle2D coronaASEJ, Rectangle2D raizASEJ, Rectangle2D recuadroASEJ, Point2D etiquetaASEJ) {

    public Rectangle2D limitesASEJ() {
        double minXASEJ = Math.min(coronaASEJ.getMinX(), Math.min(raizASEJ.getMinX(), recuadroASEJ.getMinX()));
        double minYASEJ = Math.min(coronaASEJ.getMinY(), Math.min(raizASEJ.getMinY(), recuadroASEJ.getMinY()));
        double maxXASEJ = Math.max(coronaASEJ.getMaxX(), Math.max(raizASEJ.getMaxX(), recuadroASEJ.getMaxX()));
        double maxYASEJ = Math.max(coronaASEJ.getMaxY(), Math.max(raizASEJ.getMaxY(), recuadroASEJ.getMaxY()));
        return new Rectangle2D(minXASEJ, minYASEJ, maxXASEJ - minXASEJ, maxYASEJ - minYASEJ);
    }

    public boolean contieneASEJ(double xASEJ, double yASEJ) {
        return limitesASEJ().contains(xASEJ, yASEJ);
    }
}
