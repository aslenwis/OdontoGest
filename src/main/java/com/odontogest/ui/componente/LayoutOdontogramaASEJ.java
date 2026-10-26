package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.ArcadaASEJ;
import com.odontogest.dominio.odontograma.DenticionASEJ;
import com.odontogest.dominio.odontograma.PiezaDentalASEJ;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class LayoutOdontogramaASEJ {

    public static final double MARGENASEJ = 16;
    static final double FACTOR_ALTO_FILAASEJ = 0.38;
    static final double SEPARACIONASEJ = 0.12;
    static final double LINEA_MEDIAASEJ = 0.5;
    static final double ALTO_RECUADROASEJ = 0.35;
    static final double ALTO_RAIZASEJ = 0.6;
    static final double ALTO_ETIQUETAASEJ = 0.25;
    static final double MARGEN_OCLUSALASEJ = 0.4;
    private static final double LADO_MINIMOASEJ = 1;

    private final double ladoASEJ;
    private final Map<Integer, CeldaPiezaASEJ> celdasASEJ;

    private LayoutOdontogramaASEJ(double ladoASEJ, Map<Integer, CeldaPiezaASEJ> celdasASEJ) {
        this.ladoASEJ = ladoASEJ;
        this.celdasASEJ = Collections.unmodifiableMap(celdasASEJ);
    }

    public static LayoutOdontogramaASEJ calcularASEJ(double anchoASEJ, double altoASEJ, DenticionASEJ denticionASEJ) {
        List<List<PiezaDentalASEJ>> filasASEJ = PiezaDentalASEJ.filasASEJ(denticionASEJ);
        double altoFilaASEJ = Math.max(0, altoASEJ - 2 * MARGENASEJ) / filasASEJ.size();
        double porAnchoASEJ = Math.max(0, anchoASEJ - 2 * MARGENASEJ) / (16 + 15 * SEPARACIONASEJ + LINEA_MEDIAASEJ);
        double sASEJ = Math.max(LADO_MINIMOASEJ, Math.min(porAnchoASEJ, altoFilaASEJ * FACTOR_ALTO_FILAASEJ));
        double gASEJ = SEPARACIONASEJ * sASEJ;
        double huecoASEJ = gASEJ + LINEA_MEDIAASEJ * sASEJ;
        double centroXASEJ = anchoASEJ / 2;
        double altoContenidoASEJ = (ALTO_RECUADROASEJ + ALTO_ETIQUETAASEJ + ALTO_RAIZASEJ + 1 + MARGEN_OCLUSALASEJ) * sASEJ;
        double pasoFilaASEJ = Math.min(altoFilaASEJ, sASEJ / FACTOR_ALTO_FILAASEJ);
        double arribaASEJ = (altoASEJ - filasASEJ.size() * pasoFilaASEJ) / 2;

        Map<Integer, CeldaPiezaASEJ> celdasASEJ = new LinkedHashMap<>();
        for (int rASEJ = 0; rASEJ < filasASEJ.size(); rASEJ++) {
            List<PiezaDentalASEJ> filaASEJ = filasASEJ.get(rASEJ);
            int porLadoASEJ = filaASEJ.size() / 2;
            double y0ASEJ = arribaASEJ + rASEJ * pasoFilaASEJ + (pasoFilaASEJ - altoContenidoASEJ) / 2;
            boolean superiorASEJ = filaASEJ.get(0).arcadaASEJ() == ArcadaASEJ.SUPERIOR;
            for (int iASEJ = 0; iASEJ < filaASEJ.size(); iASEJ++) {
                double xASEJ = iASEJ < porLadoASEJ
                        ? centroXASEJ - huecoASEJ / 2 - (porLadoASEJ - iASEJ) * sASEJ - (porLadoASEJ - 1 - iASEJ) * gASEJ
                        : centroXASEJ + huecoASEJ / 2 + (iASEJ - porLadoASEJ) * (sASEJ + gASEJ);
                PiezaDentalASEJ piezaASEJ = filaASEJ.get(iASEJ);
                celdasASEJ.put(piezaASEJ.fdiASEJ(), superiorASEJ ? celdaSuperiorASEJ(piezaASEJ, xASEJ, y0ASEJ, sASEJ) : celdaInferiorASEJ(piezaASEJ, xASEJ, y0ASEJ, sASEJ));
            }
        }
        return new LayoutOdontogramaASEJ(sASEJ, celdasASEJ);
    }

    private static CeldaPiezaASEJ celdaSuperiorASEJ(PiezaDentalASEJ piezaASEJ, double xASEJ, double y0ASEJ, double sASEJ) {
        Rectangle2D recuadroASEJ = new Rectangle2D(xASEJ, y0ASEJ, sASEJ, ALTO_RECUADROASEJ * sASEJ);
        Point2D etiquetaASEJ = new Point2D(xASEJ + sASEJ / 2, recuadroASEJ.getMaxY() + ALTO_ETIQUETAASEJ * sASEJ / 2);
        Rectangle2D raizASEJ = new Rectangle2D(xASEJ, recuadroASEJ.getMaxY() + ALTO_ETIQUETAASEJ * sASEJ, sASEJ, ALTO_RAIZASEJ * sASEJ);
        Rectangle2D coronaASEJ = new Rectangle2D(xASEJ, raizASEJ.getMaxY(), sASEJ, sASEJ);
        return new CeldaPiezaASEJ(piezaASEJ, coronaASEJ, raizASEJ, recuadroASEJ, etiquetaASEJ);
    }

    private static CeldaPiezaASEJ celdaInferiorASEJ(PiezaDentalASEJ piezaASEJ, double xASEJ, double y0ASEJ, double sASEJ) {
        Rectangle2D coronaASEJ = new Rectangle2D(xASEJ, y0ASEJ + MARGEN_OCLUSALASEJ * sASEJ, sASEJ, sASEJ);
        Rectangle2D raizASEJ = new Rectangle2D(xASEJ, coronaASEJ.getMaxY(), sASEJ, ALTO_RAIZASEJ * sASEJ);
        Point2D etiquetaASEJ = new Point2D(xASEJ + sASEJ / 2, raizASEJ.getMaxY() + ALTO_ETIQUETAASEJ * sASEJ / 2);
        Rectangle2D recuadroASEJ = new Rectangle2D(xASEJ, raizASEJ.getMaxY() + ALTO_ETIQUETAASEJ * sASEJ, sASEJ, ALTO_RECUADROASEJ * sASEJ);
        return new CeldaPiezaASEJ(piezaASEJ, coronaASEJ, raizASEJ, recuadroASEJ, etiquetaASEJ);
    }

    public Optional<CeldaPiezaASEJ> celdaEnASEJ(double xASEJ, double yASEJ) {
        for (CeldaPiezaASEJ cASEJ : celdasASEJ.values()) {
            if (cASEJ.contieneASEJ(xASEJ, yASEJ)) {
                return Optional.of(cASEJ);
            }
        }
        return Optional.empty();
    }

    public CeldaPiezaASEJ celdaASEJ(int fdiASEJ) {
        CeldaPiezaASEJ cASEJ = celdasASEJ.get(fdiASEJ);
        if (cASEJ == null) {
            throw new IllegalArgumentException("La pieza " + fdiASEJ + " no está en este odontograma");
        }
        return cASEJ;
    }

    public boolean contieneASEJ(int fdiASEJ) {
        return celdasASEJ.containsKey(fdiASEJ);
    }

    public Collection<CeldaPiezaASEJ> celdasASEJ() {
        return celdasASEJ.values();
    }

    public double ladoASEJ() {
        return ladoASEJ;
    }

    public double separacionASEJ() {
        return SEPARACIONASEJ * ladoASEJ;
    }
}
