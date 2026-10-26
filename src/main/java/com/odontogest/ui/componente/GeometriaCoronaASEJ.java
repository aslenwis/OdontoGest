package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.ArcadaASEJ;
import com.odontogest.dominio.odontograma.PiezaDentalASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import javafx.geometry.Rectangle2D;

public final class GeometriaCoronaASEJ {

    public static final double FACTOR_INTERIORASEJ = 0.4;

    public enum RegionASEJ {
        ARRIBA, DERECHA, ABAJO, IZQUIERDA, CENTRO
    }

    private GeometriaCoronaASEJ() {
    }

    public static RegionASEJ regionASEJ(PiezaDentalASEJ piezaASEJ, SuperficieASEJ sASEJ) {
        boolean superiorASEJ = piezaASEJ.arcadaASEJ() == ArcadaASEJ.SUPERIOR;
        boolean derechoPacienteASEJ = piezaASEJ.ladoDerechoPacienteASEJ();
        return switch (sASEJ) {
            case O -> RegionASEJ.CENTRO;
            case V -> superiorASEJ ? RegionASEJ.ARRIBA : RegionASEJ.ABAJO;
            case L -> superiorASEJ ? RegionASEJ.ABAJO : RegionASEJ.ARRIBA;
            case M -> derechoPacienteASEJ ? RegionASEJ.DERECHA : RegionASEJ.IZQUIERDA;
            case D -> derechoPacienteASEJ ? RegionASEJ.IZQUIERDA : RegionASEJ.DERECHA;
        };
    }

    public static double[][] poligonoASEJ(Rectangle2D coronaASEJ, PiezaDentalASEJ piezaASEJ, SuperficieASEJ sASEJ) {
        return poligonoASEJ(coronaASEJ, regionASEJ(piezaASEJ, sASEJ));
    }

    public static double[][] poligonoASEJ(Rectangle2D cASEJ, RegionASEJ regionASEJ) {
        double x0ASEJ = cASEJ.getMinX();
        double y0ASEJ = cASEJ.getMinY();
        double x1ASEJ = cASEJ.getMaxX();
        double y1ASEJ = cASEJ.getMaxY();
        double mASEJ = (1 - FACTOR_INTERIORASEJ) / 2 * cASEJ.getWidth();
        double ix0ASEJ = x0ASEJ + mASEJ;
        double iy0ASEJ = y0ASEJ + mASEJ;
        double ix1ASEJ = x1ASEJ - mASEJ;
        double iy1ASEJ = y1ASEJ - mASEJ;
        return switch (regionASEJ) {
            case ARRIBA -> new double[][]{{x0ASEJ, x1ASEJ, ix1ASEJ, ix0ASEJ}, {y0ASEJ, y0ASEJ, iy0ASEJ, iy0ASEJ}};
            case DERECHA -> new double[][]{{x1ASEJ, x1ASEJ, ix1ASEJ, ix1ASEJ}, {y0ASEJ, y1ASEJ, iy1ASEJ, iy0ASEJ}};
            case ABAJO -> new double[][]{{x1ASEJ, x0ASEJ, ix0ASEJ, ix1ASEJ}, {y1ASEJ, y1ASEJ, iy1ASEJ, iy1ASEJ}};
            case IZQUIERDA -> new double[][]{{x0ASEJ, x0ASEJ, ix0ASEJ, ix0ASEJ}, {y1ASEJ, y0ASEJ, iy0ASEJ, iy1ASEJ}};
            case CENTRO -> new double[][]{{ix0ASEJ, ix1ASEJ, ix1ASEJ, ix0ASEJ}, {iy0ASEJ, iy0ASEJ, iy1ASEJ, iy1ASEJ}};
        };
    }

    public static double[] centroASEJ(double[][] poligonoASEJ) {
        double cxASEJ = 0;
        double cyASEJ = 0;
        for (int iASEJ = 0; iASEJ < poligonoASEJ[0].length; iASEJ++) {
            cxASEJ += poligonoASEJ[0][iASEJ];
            cyASEJ += poligonoASEJ[1][iASEJ];
        }
        return new double[]{cxASEJ / poligonoASEJ[0].length, cyASEJ / poligonoASEJ[1].length};
    }

    public static double[] bordeExteriorASEJ(Rectangle2D cASEJ, PiezaDentalASEJ piezaASEJ, SuperficieASEJ sASEJ) {
        double[][] pASEJ = poligonoASEJ(cASEJ, regionASEJ(piezaASEJ, sASEJ));
        if (regionASEJ(piezaASEJ, sASEJ) == RegionASEJ.CENTRO) {
            double yASEJ = (pASEJ[1][0] + pASEJ[1][2]) / 2;
            return new double[]{pASEJ[0][0], yASEJ, pASEJ[0][1], yASEJ};
        }
        return new double[]{pASEJ[0][0], pASEJ[1][0], pASEJ[0][1], pASEJ[1][1]};
    }
}
