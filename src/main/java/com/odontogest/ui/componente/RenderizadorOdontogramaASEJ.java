package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.ArcadaASEJ;
import com.odontogest.dominio.odontograma.ColorHallazgoASEJ;
import com.odontogest.dominio.odontograma.DenticionASEJ;
import com.odontogest.dominio.odontograma.EstadoOdontogramaASEJ;
import com.odontogest.dominio.odontograma.HallazgoEventoASEJ;
import com.odontogest.dominio.odontograma.PiezaDentalASEJ;
import com.odontogest.dominio.odontograma.ReglaColorASEJ;
import com.odontogest.dominio.odontograma.RepresentacionASEJ;
import com.odontogest.dominio.odontograma.SimboloHallazgoASEJ;
import com.odontogest.dominio.odontograma.SuperficieASEJ;
import com.odontogest.dominio.odontograma.TipoHallazgoDefASEJ;
import com.odontogest.dominio.odontograma.TipoPiezaASEJ;
import com.odontogest.dominio.odontograma.TipoRadicularASEJ;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class RenderizadorOdontogramaASEJ {

    public record SuperposicionASEJ(Integer piezaHoverASEJ, SuperficieASEJ superficieHoverASEJ, Integer piezaSeleccionadaASEJ,
                                Integer piezaPendienteASEJ, Set<SuperficieASEJ> pendientesASEJ, Integer inicioRangoASEJ,
                                Set<Integer> diferenciasASEJ) {

        public static final SuperposicionASEJ NINGUNAASEJ = new SuperposicionASEJ(null, null, null, null, Set.of(), null, Set.of());

        public SuperposicionASEJ {
            pendientesASEJ = pendientesASEJ == null ? Set.of() : Set.copyOf(pendientesASEJ);
            diferenciasASEJ = diferenciasASEJ == null ? Set.of() : Set.copyOf(diferenciasASEJ);
        }
    }

    public void dibujarASEJ(GraphicsContext gcASEJ, double anchoASEJ, double altoASEJ, LayoutOdontogramaASEJ layoutASEJ,
                        EstadoOdontogramaASEJ estadoASEJ, PaletaOdontogramaASEJ paletaASEJ, SuperposicionASEJ supASEJ) {
        double sASEJ = layoutASEJ.ladoASEJ();
        gcASEJ.save();
        gcASEJ.setFill(paletaASEJ.fondoASEJ());
        gcASEJ.fillRect(0, 0, anchoASEJ, altoASEJ);
        gcASEJ.setLineCap(StrokeLineCap.ROUND);

        for (CeldaPiezaASEJ cASEJ : layoutASEJ.celdasASEJ()) {
            if (supASEJ.piezaHoverASEJ() != null && supASEJ.piezaHoverASEJ() == cASEJ.piezaASEJ().fdiASEJ()) {
                gcASEJ.setFill(paletaASEJ.conTransparenciaASEJ(paletaASEJ.acentoASEJ(), 0.08));
                Rectangle2D lASEJ = expandirASEJ(cASEJ.limitesASEJ(), 0.06 * sASEJ);
                gcASEJ.fillRoundRect(lASEJ.getMinX(), lASEJ.getMinY(), lASEJ.getWidth(), lASEJ.getHeight(), 0.2 * sASEJ, 0.2 * sASEJ);
            }
        }
        for (CeldaPiezaASEJ cASEJ : layoutASEJ.celdasASEJ()) {
            dibujarPiezaASEJ(gcASEJ, cASEJ, estadoASEJ, paletaASEJ, sASEJ);
        }
        for (HallazgoEventoASEJ rASEJ : estadoASEJ.rangosASEJ()) {
            List<CeldaPiezaASEJ> celdasASEJ = PiezaDentalASEJ.rangoASEJ(rASEJ.piezaASEJ(), rASEJ.piezaFinASEJ()).stream()
                    .filter(pASEJ -> layoutASEJ.contieneASEJ(pASEJ.fdiASEJ())).map(pASEJ -> layoutASEJ.celdaASEJ(pASEJ.fdiASEJ()))
                    .sorted(Comparator.comparingDouble(cASEJ -> cASEJ.coronaASEJ().getMinX())).toList();
            if (!celdasASEJ.isEmpty()) {
                dibujarRangoASEJ(gcASEJ, rASEJ.tipoASEJ().simboloASEJ(), paletaASEJ.colorASEJ(rASEJ.colorASEJ()), celdasASEJ.get(0), celdasASEJ.get(celdasASEJ.size() - 1), sASEJ);
            }
        }
        dibujarSuperposicionASEJ(gcASEJ, layoutASEJ, paletaASEJ, supASEJ, sASEJ);
        gcASEJ.restore();
    }

    public WritableImage imagenASEJ(int anchoASEJ, int altoASEJ, DenticionASEJ denticionASEJ, EstadoOdontogramaASEJ estadoASEJ, Set<Integer> diferenciasASEJ) {
        Canvas canvasASEJ = new Canvas(anchoASEJ, altoASEJ);
        LayoutOdontogramaASEJ layoutASEJ = LayoutOdontogramaASEJ.calcularASEJ(anchoASEJ, altoASEJ, denticionASEJ);
        dibujarASEJ(canvasASEJ.getGraphicsContext2D(), anchoASEJ, altoASEJ, layoutASEJ, estadoASEJ, PaletaOdontogramaASEJ.claraASEJ(),
                new SuperposicionASEJ(null, null, null, null, Set.of(), null, diferenciasASEJ));
        SnapshotParameters parametrosASEJ = new SnapshotParameters();
        parametrosASEJ.setFill(Color.WHITE);
        return canvasASEJ.snapshot(parametrosASEJ, null);
    }

    private void dibujarPiezaASEJ(GraphicsContext gcASEJ, CeldaPiezaASEJ cASEJ, EstadoOdontogramaASEJ estadoASEJ, PaletaOdontogramaASEJ paletaASEJ, double sASEJ) {
        int fdiASEJ = cASEJ.piezaASEJ().fdiASEJ();
        double lwASEJ = grosorASEJ(sASEJ);
        Rectangle2D rASEJ = cASEJ.recuadroASEJ();
        gcASEJ.setStroke(paletaASEJ.trazoASEJ());
        gcASEJ.setLineWidth(lwASEJ);
        gcASEJ.strokeRect(rASEJ.getMinX(), rASEJ.getMinY(), rASEJ.getWidth(), rASEJ.getHeight());
        textoASEJ(gcASEJ, String.valueOf(fdiASEJ), cASEJ.etiquetaASEJ().getX(), cASEJ.etiquetaASEJ().getY(), 0.22 * sASEJ, false, paletaASEJ.textoASEJ());

        boolean ocultasASEJ = estadoASEJ.superficiesOcultasASEJ(fdiASEJ);
        List<HallazgoEventoASEJ> visiblesASEJ = estadoASEJ.hallazgosVisiblesASEJ(fdiASEJ);
        if (!ocultasASEJ) {
            for (int capaASEJ = 0; capaASEJ < 2; capaASEJ++) {
                for (HallazgoEventoASEJ hASEJ : visiblesASEJ) {
                    if (!hASEJ.esDeSuperficieASEJ()) {
                        continue;
                    }
                    boolean rellenoASEJ = hASEJ.tipoASEJ().simboloASEJ() == SimboloHallazgoASEJ.RELLENO;
                    if ((capaASEJ == 0) == rellenoASEJ) {
                        for (SuperficieASEJ supASEJ : hASEJ.superficiesASEJ()) {
                            dibujarSuperficieASEJ(gcASEJ, hASEJ.tipoASEJ().simboloASEJ(), paletaASEJ.colorASEJ(hASEJ.colorASEJ()), cASEJ.coronaASEJ(), cASEJ.piezaASEJ(), supASEJ, sASEJ);
                        }
                    }
                }
            }
        }
        contornoCoronaASEJ(gcASEJ, cASEJ.coronaASEJ(), paletaASEJ.trazoASEJ(), lwASEJ);
        raicesASEJ(gcASEJ, cASEJ, paletaASEJ.trazoASEJ(), lwASEJ);
        for (HallazgoEventoASEJ hASEJ : visiblesASEJ) {
            if (!hASEJ.esDeSuperficieASEJ() && !hASEJ.esDeRangoASEJ()) {
                dibujarSimboloPiezaASEJ(gcASEJ, hASEJ.tipoASEJ().simboloASEJ(), paletaASEJ.colorASEJ(hASEJ.colorASEJ()), cASEJ, sASEJ);
            }
        }
        siglasASEJ(gcASEJ, cASEJ, visiblesASEJ, paletaASEJ, sASEJ);
    }

    private static void contornoCoronaASEJ(GraphicsContext gcASEJ, Rectangle2D cASEJ, Color colorASEJ, double lwASEJ) {
        gcASEJ.setStroke(colorASEJ);
        gcASEJ.setLineWidth(lwASEJ);
        gcASEJ.strokeRect(cASEJ.getMinX(), cASEJ.getMinY(), cASEJ.getWidth(), cASEJ.getHeight());
        double[][] centroASEJ = GeometriaCoronaASEJ.poligonoASEJ(cASEJ, GeometriaCoronaASEJ.RegionASEJ.CENTRO);
        gcASEJ.strokeRect(centroASEJ[0][0], centroASEJ[1][0], centroASEJ[0][1] - centroASEJ[0][0], centroASEJ[1][2] - centroASEJ[1][0]);
        gcASEJ.strokeLine(cASEJ.getMinX(), cASEJ.getMinY(), centroASEJ[0][0], centroASEJ[1][0]);
        gcASEJ.strokeLine(cASEJ.getMaxX(), cASEJ.getMinY(), centroASEJ[0][1], centroASEJ[1][1]);
        gcASEJ.strokeLine(cASEJ.getMaxX(), cASEJ.getMaxY(), centroASEJ[0][2], centroASEJ[1][2]);
        gcASEJ.strokeLine(cASEJ.getMinX(), cASEJ.getMaxY(), centroASEJ[0][3], centroASEJ[1][3]);
    }

    private static void raicesASEJ(GraphicsContext gcASEJ, CeldaPiezaASEJ cASEJ, Color colorASEJ, double lwASEJ) {
        PiezaDentalASEJ pASEJ = cASEJ.piezaASEJ();
        boolean superiorASEJ = pASEJ.arcadaASEJ() == ArcadaASEJ.SUPERIOR;
        int nASEJ = pASEJ.tipoASEJ() == TipoPiezaASEJ.MOLAR ? (superiorASEJ ? 3 : 2) : pASEJ.tipoRadicularASEJ() == TipoRadicularASEJ.BIRRADICULAR ? 2 : 1;
        Rectangle2D rASEJ = cASEJ.raizASEJ();
        double baseASEJ = superiorASEJ ? rASEJ.getMaxY() : rASEJ.getMinY();
        double apiceASEJ = superiorASEJ ? rASEJ.getMinY() : rASEJ.getMaxY();
        double margenASEJ = rASEJ.getWidth() * 0.08;
        double wASEJ = (rASEJ.getWidth() - 2 * margenASEJ) / nASEJ;
        gcASEJ.setStroke(colorASEJ);
        gcASEJ.setLineWidth(lwASEJ);
        for (int iASEJ = 0; iASEJ < nASEJ; iASEJ++) {
            double x0ASEJ = rASEJ.getMinX() + margenASEJ + iASEJ * wASEJ;
            gcASEJ.strokePolyline(new double[]{x0ASEJ, x0ASEJ + wASEJ / 2, x0ASEJ + wASEJ}, new double[]{baseASEJ, apiceASEJ, baseASEJ}, 3);
        }
    }

    private static void dibujarSuperficieASEJ(GraphicsContext gcASEJ, SimboloHallazgoASEJ simboloASEJ, Color colorASEJ, Rectangle2D coronaASEJ,
                                          PiezaDentalASEJ piezaASEJ, SuperficieASEJ supASEJ, double sASEJ) {
        double[][] pASEJ = GeometriaCoronaASEJ.poligonoASEJ(coronaASEJ, piezaASEJ, supASEJ);
        gcASEJ.setStroke(colorASEJ);
        gcASEJ.setFill(colorASEJ);
        switch (simboloASEJ) {
            case RELLENO -> gcASEJ.fillPolygon(pASEJ[0], pASEJ[1], 4);
            case CONTORNO_SUPERFICIE -> {
                gcASEJ.setLineWidth(grosorASEJ(sASEJ) * 2.6);
                gcASEJ.strokePolygon(pASEJ[0], pASEJ[1], 4);
            }
            case TRAZO_FOSAS -> {
                gcASEJ.setLineWidth(grosorASEJ(sASEJ) * 1.8);
                double[] cenASEJ = GeometriaCoronaASEJ.centroASEJ(pASEJ);
                double dASEJ = 0.13 * sASEJ;
                gcASEJ.strokeLine(cenASEJ[0] - dASEJ, cenASEJ[1], cenASEJ[0] + dASEJ, cenASEJ[1]);
                gcASEJ.strokeLine(cenASEJ[0], cenASEJ[1] - dASEJ, cenASEJ[0], cenASEJ[1] + dASEJ);
            }
            case LINEA_DESGASTE -> {
                double[] bASEJ = GeometriaCoronaASEJ.bordeExteriorASEJ(coronaASEJ, piezaASEJ, supASEJ);
                gcASEJ.setLineWidth(grosorASEJ(sASEJ) * 3.2);
                gcASEJ.strokeLine(bASEJ[0], bASEJ[1], bASEJ[2], bASEJ[3]);
            }
            default -> gcASEJ.fillPolygon(pASEJ[0], pASEJ[1], 4);
        }
    }

    private static void dibujarSimboloPiezaASEJ(GraphicsContext gcASEJ, SimboloHallazgoASEJ simboloASEJ, Color colorASEJ, CeldaPiezaASEJ cASEJ, double sASEJ) {
        Rectangle2D coASEJ = cASEJ.coronaASEJ();
        Rectangle2D raASEJ = cASEJ.raizASEJ();
        boolean superiorASEJ = cASEJ.piezaASEJ().arcadaASEJ() == ArcadaASEJ.SUPERIOR;
        double dirASEJ = superiorASEJ ? 1 : -1;
        double bordeOclusalASEJ = superiorASEJ ? coASEJ.getMaxY() : coASEJ.getMinY();
        double apiceASEJ = superiorASEJ ? raASEJ.getMinY() : raASEJ.getMaxY();
        double cxASEJ = coASEJ.getMinX() + coASEJ.getWidth() / 2;
        double lwASEJ = grosorASEJ(sASEJ);
        gcASEJ.setStroke(colorASEJ);
        gcASEJ.setFill(colorASEJ);
        gcASEJ.setLineWidth(lwASEJ * 2.4);
        switch (simboloASEJ) {
            case ASPA -> {
                double y0ASEJ = Math.min(coASEJ.getMinY(), raASEJ.getMinY());
                double y1ASEJ = Math.max(coASEJ.getMaxY(), raASEJ.getMaxY());
                gcASEJ.strokeLine(coASEJ.getMinX(), y0ASEJ, coASEJ.getMaxX(), y1ASEJ);
                gcASEJ.strokeLine(coASEJ.getMaxX(), y0ASEJ, coASEJ.getMinX(), y1ASEJ);
            }
            case CUADRADO_CORONA -> {
                Rectangle2D eASEJ = expandirASEJ(coASEJ, 0.07 * sASEJ);
                gcASEJ.strokeRect(eASEJ.getMinX(), eASEJ.getMinY(), eASEJ.getWidth(), eASEJ.getHeight());
            }
            case LINEA_RAIZ -> gcASEJ.strokeLine(cxASEJ, superiorASEJ ? coASEJ.getMinY() : coASEJ.getMaxY(), cxASEJ, apiceASEJ + dirASEJ * 0.06 * sASEJ);
            case ESPIGO_MUNON -> {
                double lASEJ = 0.34 * sASEJ;
                double yASEJ = coASEJ.getMinY() + (sASEJ - lASEJ) / 2;
                gcASEJ.strokeRect(cxASEJ - lASEJ / 2, yASEJ, lASEJ, lASEJ);
                gcASEJ.strokeLine(cxASEJ, superiorASEJ ? yASEJ : yASEJ + lASEJ, cxASEJ, apiceASEJ + dirASEJ * 0.1 * sASEJ);
            }
            case PULPA_CORONAL -> {
                double lASEJ = 0.3 * sASEJ;
                gcASEJ.fillRect(cxASEJ - lASEJ / 2, coASEJ.getMinY() + (sASEJ - lASEJ) / 2, lASEJ, lASEJ);
            }
            case FLECHA_EXTRUSION -> flechaASEJ(gcASEJ, cxASEJ, bordeOclusalASEJ + dirASEJ * 0.06 * sASEJ, cxASEJ, bordeOclusalASEJ + dirASEJ * 0.36 * sASEJ, sASEJ);
            case FLECHA_INTRUSION -> flechaASEJ(gcASEJ, cxASEJ, bordeOclusalASEJ + dirASEJ * 0.36 * sASEJ, cxASEJ, bordeOclusalASEJ + dirASEJ * 0.06 * sASEJ, sASEJ);
            case FLECHA_GIRO -> {
                double yASEJ = bordeOclusalASEJ + dirASEJ * 0.1 * sASEJ;
                double xcASEJ = cxASEJ;
                double ycASEJ = bordeOclusalASEJ + dirASEJ * 0.36 * sASEJ;
                gcASEJ.beginPath();
                gcASEJ.moveTo(cxASEJ - 0.3 * sASEJ, yASEJ);
                gcASEJ.quadraticCurveTo(xcASEJ, ycASEJ, cxASEJ + 0.3 * sASEJ, yASEJ);
                gcASEJ.stroke();
                puntaASEJ(gcASEJ, cxASEJ + 0.3 * sASEJ, yASEJ, cxASEJ + 0.3 * sASEJ - xcASEJ, yASEJ - ycASEJ, sASEJ);
            }
            case FLECHA_ZIGZAG -> {
                double desdeASEJ = apiceASEJ - dirASEJ * 0.05 * sASEJ;
                double hastaASEJ = bordeOclusalASEJ + dirASEJ * 0.12 * sASEJ;
                int pasosASEJ = 6;
                double[] xsASEJ = new double[pasosASEJ + 1];
                double[] ysASEJ = new double[pasosASEJ + 1];
                for (int iASEJ = 0; iASEJ <= pasosASEJ; iASEJ++) {
                    xsASEJ[iASEJ] = cxASEJ + (iASEJ == pasosASEJ ? 0 : (iASEJ % 2 == 0 ? -1 : 1) * 0.16 * sASEJ);
                    ysASEJ[iASEJ] = desdeASEJ + (hastaASEJ - desdeASEJ) * iASEJ / pasosASEJ;
                }
                gcASEJ.strokePolyline(xsASEJ, ysASEJ, xsASEJ.length);
                puntaASEJ(gcASEJ, xsASEJ[pasosASEJ], ysASEJ[pasosASEJ], 0, dirASEJ, sASEJ);
            }
            case LINEA_FRACTURA -> gcASEJ.strokeLine(coASEJ.getMinX() + 0.12 * sASEJ, coASEJ.getMinY() - (superiorASEJ ? 0.25 * sASEJ : 0),
                    coASEJ.getMaxX() - 0.12 * sASEJ, coASEJ.getMaxY() + (superiorASEJ ? 0 : 0.25 * sASEJ));
            case TRIANGULO_RAIZ -> {
                double hASEJ = 0.32 * sASEJ;
                double y0ASEJ = apiceASEJ - dirASEJ * 0.02 * sASEJ;
                gcASEJ.strokePolygon(new double[]{cxASEJ - 0.2 * sASEJ, cxASEJ + 0.2 * sASEJ, cxASEJ},
                        new double[]{y0ASEJ + dirASEJ * hASEJ, y0ASEJ + dirASEJ * hASEJ, y0ASEJ}, 3);
            }
            case CIRCULO_NUMERO -> {
                Point2D eASEJ = cASEJ.etiquetaASEJ();
                gcASEJ.setLineWidth(lwASEJ * 1.8);
                gcASEJ.strokeOval(eASEJ.getX() - 0.3 * sASEJ, eASEJ.getY() - 0.17 * sASEJ, 0.6 * sASEJ, 0.34 * sASEJ);
            }
            default -> {
            }
        }
    }

    private static void dibujarRangoASEJ(GraphicsContext gcASEJ, SimboloHallazgoASEJ simboloASEJ, Color colorASEJ, CeldaPiezaASEJ izqASEJ,
                                     CeldaPiezaASEJ derASEJ, double sASEJ) {
        boolean superiorASEJ = izqASEJ.piezaASEJ().arcadaASEJ() == ArcadaASEJ.SUPERIOR;
        double haciaCoronaASEJ = superiorASEJ ? 1 : -1;
        double apiceASEJ = superiorASEJ ? izqASEJ.raizASEJ().getMinY() + 0.12 * sASEJ : izqASEJ.raizASEJ().getMaxY() - 0.12 * sASEJ;
        double x0ASEJ = izqASEJ.coronaASEJ().getMinX();
        double x1ASEJ = derASEJ.coronaASEJ().getMaxX();
        double c0ASEJ = izqASEJ.coronaASEJ().getMinX() + sASEJ / 2;
        double c1ASEJ = derASEJ.coronaASEJ().getMinX() + sASEJ / 2;
        double lwASEJ = grosorASEJ(sASEJ);
        gcASEJ.setStroke(colorASEJ);
        gcASEJ.setFill(colorASEJ);
        gcASEJ.setLineWidth(lwASEJ * 2.4);
        switch (simboloASEJ) {
            case PUENTE -> {
                gcASEJ.strokeLine(c0ASEJ, apiceASEJ, c1ASEJ, apiceASEJ);
                gcASEJ.strokeLine(c0ASEJ, apiceASEJ, c0ASEJ, apiceASEJ + haciaCoronaASEJ * 0.28 * sASEJ);
                gcASEJ.strokeLine(c1ASEJ, apiceASEJ, c1ASEJ, apiceASEJ + haciaCoronaASEJ * 0.28 * sASEJ);
            }
            case LINEAS_PARALELAS -> {
                gcASEJ.strokeLine(x0ASEJ, apiceASEJ - 0.06 * sASEJ, x1ASEJ, apiceASEJ - 0.06 * sASEJ);
                gcASEJ.strokeLine(x0ASEJ, apiceASEJ + 0.06 * sASEJ, x1ASEJ, apiceASEJ + 0.06 * sASEJ);
            }
            case CUADROS_APICES -> {
                double lASEJ = 0.24 * sASEJ;
                for (double cxASEJ : new double[]{c0ASEJ, c1ASEJ}) {
                    gcASEJ.strokeRect(cxASEJ - lASEJ / 2, apiceASEJ - lASEJ / 2, lASEJ, lASEJ);
                    gcASEJ.setLineWidth(lwASEJ * 1.4);
                    gcASEJ.strokeLine(cxASEJ - lASEJ / 2, apiceASEJ, cxASEJ + lASEJ / 2, apiceASEJ);
                    gcASEJ.strokeLine(cxASEJ, apiceASEJ - lASEJ / 2, cxASEJ, apiceASEJ + lASEJ / 2);
                    gcASEJ.setLineWidth(lwASEJ * 2.4);
                }
                gcASEJ.strokeLine(c0ASEJ + lASEJ / 2, apiceASEJ, c1ASEJ - lASEJ / 2, apiceASEJ);
            }
            case ZIGZAG_APICES -> {
                int pasosASEJ = Math.max(2, (int) Math.round((x1ASEJ - x0ASEJ) / (0.18 * sASEJ)));
                double[] xsASEJ = new double[pasosASEJ + 1];
                double[] ysASEJ = new double[pasosASEJ + 1];
                for (int iASEJ = 0; iASEJ <= pasosASEJ; iASEJ++) {
                    xsASEJ[iASEJ] = x0ASEJ + (x1ASEJ - x0ASEJ) * iASEJ / pasosASEJ;
                    ysASEJ[iASEJ] = apiceASEJ + (iASEJ % 2 == 0 ? -1 : 1) * 0.07 * sASEJ;
                }
                gcASEJ.strokePolyline(xsASEJ, ysASEJ, xsASEJ.length);
            }
            case LINEA_CORONAS -> {
                double yASEJ = izqASEJ.coronaASEJ().getMinY() + sASEJ / 2;
                gcASEJ.strokeLine(x0ASEJ - 0.05 * sASEJ, yASEJ, x1ASEJ + 0.05 * sASEJ, yASEJ);
            }
            case PARENTESIS -> {
                double gxASEJ = (izqASEJ.coronaASEJ().getMaxX() + derASEJ.coronaASEJ().getMinX()) / 2;
                double y0ASEJ = izqASEJ.coronaASEJ().getMinY() + 0.1 * sASEJ;
                double y1ASEJ = izqASEJ.coronaASEJ().getMaxY() - 0.1 * sASEJ;
                double ymASEJ = (y0ASEJ + y1ASEJ) / 2;
                gcASEJ.beginPath();
                gcASEJ.moveTo(gxASEJ - 0.14 * sASEJ, y0ASEJ);
                gcASEJ.quadraticCurveTo(gxASEJ + 0.14 * sASEJ, ymASEJ, gxASEJ - 0.14 * sASEJ, y1ASEJ);
                gcASEJ.moveTo(gxASEJ + 0.14 * sASEJ, y0ASEJ);
                gcASEJ.quadraticCurveTo(gxASEJ - 0.14 * sASEJ, ymASEJ, gxASEJ + 0.14 * sASEJ, y1ASEJ);
                gcASEJ.stroke();
            }
            case CIRCULOS_NUMEROS -> {
                gcASEJ.setLineWidth(lwASEJ * 1.8);
                double rASEJ = Math.abs(derASEJ.etiquetaASEJ().getX() - izqASEJ.etiquetaASEJ().getX()) * 0.62;
                for (CeldaPiezaASEJ cASEJ : List.of(izqASEJ, derASEJ)) {
                    gcASEJ.strokeOval(cASEJ.etiquetaASEJ().getX() - rASEJ, cASEJ.etiquetaASEJ().getY() - 0.2 * sASEJ, 2 * rASEJ, 0.4 * sASEJ);
                }
            }
            case CIRCULO_S -> {
                double gxASEJ = (izqASEJ.coronaASEJ().getMaxX() + derASEJ.coronaASEJ().getMinX()) / 2;
                double rASEJ = 0.17 * sASEJ;
                gcASEJ.setLineWidth(lwASEJ * 1.6);
                gcASEJ.strokeOval(gxASEJ - rASEJ, apiceASEJ - rASEJ, 2 * rASEJ, 2 * rASEJ);
                textoASEJ(gcASEJ, "S", gxASEJ, apiceASEJ, 0.2 * sASEJ, true, colorASEJ);
            }
            case FLECHAS_CRUZADAS -> {
                double yASEJ = izqASEJ.etiquetaASEJ().getY();
                double xaASEJ = izqASEJ.etiquetaASEJ().getX();
                double xbASEJ = derASEJ.etiquetaASEJ().getX();
                double altoASEJ = 0.22 * sASEJ;
                gcASEJ.setLineWidth(lwASEJ * 1.6);
                gcASEJ.beginPath();
                gcASEJ.moveTo(xaASEJ, yASEJ - altoASEJ * 0.3);
                gcASEJ.quadraticCurveTo((xaASEJ + xbASEJ) / 2, yASEJ - altoASEJ * 1.6, xbASEJ, yASEJ + altoASEJ * 0.3);
                gcASEJ.stroke();
                puntaASEJ(gcASEJ, xbASEJ, yASEJ + altoASEJ * 0.3, xbASEJ - (xaASEJ + xbASEJ) / 2, altoASEJ * 1.9, sASEJ * 0.8);
                gcASEJ.beginPath();
                gcASEJ.moveTo(xbASEJ, yASEJ - altoASEJ * 0.3);
                gcASEJ.quadraticCurveTo((xaASEJ + xbASEJ) / 2, yASEJ - altoASEJ * 1.6, xaASEJ, yASEJ + altoASEJ * 0.3);
                gcASEJ.stroke();
                puntaASEJ(gcASEJ, xaASEJ, yASEJ + altoASEJ * 0.3, xaASEJ - (xaASEJ + xbASEJ) / 2, altoASEJ * 1.9, sASEJ * 0.8);
            }
            default -> {
            }
        }
    }

    private static void siglasASEJ(GraphicsContext gcASEJ, CeldaPiezaASEJ cASEJ, List<HallazgoEventoASEJ> visiblesASEJ, PaletaOdontogramaASEJ paletaASEJ, double sASEJ) {
        List<String> textosASEJ = new ArrayList<>();
        List<Color> coloresASEJ = new ArrayList<>();
        for (HallazgoEventoASEJ hASEJ : visiblesASEJ) {
            String siglaASEJ = hASEJ.siglaASEJ() != null ? hASEJ.siglaASEJ() : hASEJ.tipoASEJ().siglaASEJ();
            if (siglaASEJ != null && !hASEJ.esDeRangoASEJ() && !textosASEJ.contains(siglaASEJ)) {
                textosASEJ.add(siglaASEJ);
                coloresASEJ.add(paletaASEJ.colorASEJ(hASEJ.colorASEJ()));
            }
        }
        if (textosASEJ.isEmpty()) {
            return;
        }
        Rectangle2D rASEJ = cASEJ.recuadroASEJ();
        double tamASEJ = 0.24 * sASEJ;
        double anchoASEJ = medirASEJ(String.join(" ", textosASEJ), tamASEJ, true);
        if (anchoASEJ > rASEJ.getWidth() * 0.92) {
            tamASEJ *= rASEJ.getWidth() * 0.92 / anchoASEJ;
            anchoASEJ = medirASEJ(String.join(" ", textosASEJ), tamASEJ, true);
        }
        double xASEJ = rASEJ.getMinX() + (rASEJ.getWidth() - anchoASEJ) / 2;
        double espacioASEJ = medirASEJ(" ", tamASEJ, true);
        for (int iASEJ = 0; iASEJ < textosASEJ.size(); iASEJ++) {
            double wASEJ = medirASEJ(textosASEJ.get(iASEJ), tamASEJ, true);
            textoASEJ(gcASEJ, textosASEJ.get(iASEJ), xASEJ + wASEJ / 2, rASEJ.getMinY() + rASEJ.getHeight() / 2, tamASEJ, true, coloresASEJ.get(iASEJ));
            xASEJ += wASEJ + espacioASEJ;
        }
    }

    private static void dibujarSuperposicionASEJ(GraphicsContext gcASEJ, LayoutOdontogramaASEJ layoutASEJ, PaletaOdontogramaASEJ paletaASEJ,
                                             SuperposicionASEJ supASEJ, double sASEJ) {
        double lwASEJ = grosorASEJ(sASEJ);
        if (supASEJ.piezaPendienteASEJ() != null && layoutASEJ.contieneASEJ(supASEJ.piezaPendienteASEJ())) {
            CeldaPiezaASEJ cASEJ = layoutASEJ.celdaASEJ(supASEJ.piezaPendienteASEJ());
            for (SuperficieASEJ superficieASEJ : supASEJ.pendientesASEJ()) {
                double[][] pASEJ = GeometriaCoronaASEJ.poligonoASEJ(cASEJ.coronaASEJ(), cASEJ.piezaASEJ(), superficieASEJ);
                gcASEJ.setFill(paletaASEJ.conTransparenciaASEJ(paletaASEJ.acentoASEJ(), 0.3));
                gcASEJ.fillPolygon(pASEJ[0], pASEJ[1], 4);
                gcASEJ.setStroke(paletaASEJ.acentoASEJ());
                gcASEJ.setLineWidth(lwASEJ * 1.6);
                gcASEJ.setLineDashes(0.08 * sASEJ, 0.06 * sASEJ);
                gcASEJ.strokePolygon(pASEJ[0], pASEJ[1], 4);
                gcASEJ.setLineDashes((double[]) null);
            }
        }
        if (supASEJ.piezaHoverASEJ() != null && supASEJ.superficieHoverASEJ() != null && layoutASEJ.contieneASEJ(supASEJ.piezaHoverASEJ())) {
            CeldaPiezaASEJ cASEJ = layoutASEJ.celdaASEJ(supASEJ.piezaHoverASEJ());
            double[][] pASEJ = GeometriaCoronaASEJ.poligonoASEJ(cASEJ.coronaASEJ(), cASEJ.piezaASEJ(), supASEJ.superficieHoverASEJ());
            gcASEJ.setFill(paletaASEJ.conTransparenciaASEJ(paletaASEJ.acentoASEJ(), 0.35));
            gcASEJ.fillPolygon(pASEJ[0], pASEJ[1], 4);
        }
        for (int fdiASEJ : supASEJ.diferenciasASEJ()) {
            if (layoutASEJ.contieneASEJ(fdiASEJ)) {
                marcoASEJ(gcASEJ, layoutASEJ.celdaASEJ(fdiASEJ), paletaASEJ.diferenciaASEJ(), lwASEJ * 2.6, 0.1 * sASEJ, false, sASEJ);
            }
        }
        if (supASEJ.inicioRangoASEJ() != null && layoutASEJ.contieneASEJ(supASEJ.inicioRangoASEJ())) {
            marcoASEJ(gcASEJ, layoutASEJ.celdaASEJ(supASEJ.inicioRangoASEJ()), paletaASEJ.acentoASEJ(), lwASEJ * 2, 0.06 * sASEJ, true, sASEJ);
        }
        if (supASEJ.piezaSeleccionadaASEJ() != null && layoutASEJ.contieneASEJ(supASEJ.piezaSeleccionadaASEJ())) {
            marcoASEJ(gcASEJ, layoutASEJ.celdaASEJ(supASEJ.piezaSeleccionadaASEJ()), paletaASEJ.acentoASEJ(), lwASEJ * 2, 0.06 * sASEJ, false, sASEJ);
        }
    }

    public void muestraASEJ(GraphicsContext gcASEJ, TipoHallazgoDefASEJ tipoASEJ, double ladoASEJ, PaletaOdontogramaASEJ paletaASEJ) {
        gcASEJ.save();
        gcASEJ.clearRect(0, 0, ladoASEJ, ladoASEJ);
        Color colorASEJ = tipoASEJ.reglaColorASEJ() == ReglaColorASEJ.SIEMPRE_ROJO ? paletaASEJ.rojoASEJ() : paletaASEJ.azulASEJ();
        double lwASEJ = Math.max(1, ladoASEJ / 28);
        if (tipoASEJ.representacionASEJ() == RepresentacionASEJ.ENTRE_PIEZAS || tipoASEJ.representacionASEJ() == RepresentacionASEJ.RANGO_PIEZAS) {
            double sASEJ = ladoASEJ * 0.4;
            CeldaPiezaASEJ aASEJ = celdaMuestraASEJ(PiezaDentalASEJ.deASEJ(12), ladoASEJ * 0.06, ladoASEJ, sASEJ);
            CeldaPiezaASEJ bASEJ = celdaMuestraASEJ(PiezaDentalASEJ.deASEJ(11), ladoASEJ * 0.54, ladoASEJ, sASEJ);
            for (CeldaPiezaASEJ cASEJ : List.of(aASEJ, bASEJ)) {
                contornoCoronaASEJ(gcASEJ, cASEJ.coronaASEJ(), paletaASEJ.trazoASEJ(), lwASEJ * 0.7);
                raicesASEJ(gcASEJ, cASEJ, paletaASEJ.trazoASEJ(), lwASEJ * 0.7);
            }
            dibujarRangoASEJ(gcASEJ, tipoASEJ.simboloASEJ(), colorASEJ, aASEJ, bASEJ, sASEJ);
        } else {
            double sASEJ = ladoASEJ * 0.5;
            CeldaPiezaASEJ cASEJ = celdaMuestraASEJ(PiezaDentalASEJ.deASEJ(16), (ladoASEJ - sASEJ) / 2, ladoASEJ, sASEJ);
            if (tipoASEJ.representacionASEJ() == RepresentacionASEJ.SUPERFICIE) {
                dibujarSuperficieASEJ(gcASEJ, tipoASEJ.simboloASEJ(), colorASEJ, cASEJ.coronaASEJ(), cASEJ.piezaASEJ(), SuperficieASEJ.O, sASEJ);
            }
            contornoCoronaASEJ(gcASEJ, cASEJ.coronaASEJ(), paletaASEJ.trazoASEJ(), lwASEJ * 0.7);
            raicesASEJ(gcASEJ, cASEJ, paletaASEJ.trazoASEJ(), lwASEJ * 0.7);
            if (tipoASEJ.representacionASEJ() == RepresentacionASEJ.PIEZA) {
                dibujarSimboloPiezaASEJ(gcASEJ, tipoASEJ.simboloASEJ(), colorASEJ, cASEJ, sASEJ);
            }
            String siglaASEJ = tipoASEJ.siglaASEJ() != null ? tipoASEJ.siglaASEJ() : tipoASEJ.siglasOpcionesASEJ().isEmpty() ? null : tipoASEJ.siglasOpcionesASEJ().get(0);
            if (tipoASEJ.simboloASEJ() == SimboloHallazgoASEJ.SOLO_SIGLA && siglaASEJ != null) {
                gcASEJ.setFill(paletaASEJ.conTransparenciaASEJ(paletaASEJ.sinFondoASEJ(), 0.85));
                gcASEJ.fillRect(cASEJ.coronaASEJ().getMinX() - 2, cASEJ.coronaASEJ().getMinY() + sASEJ * 0.2, sASEJ + 4, sASEJ * 0.6);
                textoASEJ(gcASEJ, siglaASEJ, cASEJ.coronaASEJ().getMinX() + sASEJ / 2, cASEJ.coronaASEJ().getMinY() + sASEJ / 2, sASEJ * 0.45, true, colorASEJ);
            }
        }
        gcASEJ.restore();
    }

    private static CeldaPiezaASEJ celdaMuestraASEJ(PiezaDentalASEJ piezaASEJ, double xASEJ, double ladoASEJ, double sASEJ) {
        double yCoronaASEJ = ladoASEJ - sASEJ - ladoASEJ * 0.08;
        Rectangle2D coronaASEJ = new Rectangle2D(xASEJ, yCoronaASEJ, sASEJ, sASEJ);
        Rectangle2D raizASEJ = new Rectangle2D(xASEJ, yCoronaASEJ - 0.6 * sASEJ, sASEJ, 0.6 * sASEJ);
        Rectangle2D recuadroASEJ = new Rectangle2D(xASEJ, 0, sASEJ, 0.01);
        return new CeldaPiezaASEJ(piezaASEJ, coronaASEJ, raizASEJ, recuadroASEJ, new Point2D(xASEJ + sASEJ / 2, raizASEJ.getMinY() - 0.12 * sASEJ));
    }

    private static double grosorASEJ(double sASEJ) {
        return Math.max(1, sASEJ * 0.025);
    }

    private static Rectangle2D expandirASEJ(Rectangle2D rASEJ, double dASEJ) {
        return new Rectangle2D(rASEJ.getMinX() - dASEJ, rASEJ.getMinY() - dASEJ, rASEJ.getWidth() + 2 * dASEJ, rASEJ.getHeight() + 2 * dASEJ);
    }

    private static void marcoASEJ(GraphicsContext gcASEJ, CeldaPiezaASEJ cASEJ, Color colorASEJ, double lwASEJ, double margenASEJ, boolean discontinuoASEJ, double sASEJ) {
        Rectangle2D lASEJ = expandirASEJ(cASEJ.limitesASEJ(), margenASEJ);
        gcASEJ.setStroke(colorASEJ);
        gcASEJ.setLineWidth(lwASEJ);
        if (discontinuoASEJ) {
            gcASEJ.setLineDashes(0.1 * sASEJ, 0.07 * sASEJ);
        }
        gcASEJ.strokeRoundRect(lASEJ.getMinX(), lASEJ.getMinY(), lASEJ.getWidth(), lASEJ.getHeight(), 0.15 * sASEJ, 0.15 * sASEJ);
        gcASEJ.setLineDashes((double[]) null);
    }

    private static void flechaASEJ(GraphicsContext gcASEJ, double x0ASEJ, double y0ASEJ, double x1ASEJ, double y1ASEJ, double sASEJ) {
        gcASEJ.strokeLine(x0ASEJ, y0ASEJ, x1ASEJ, y1ASEJ);
        puntaASEJ(gcASEJ, x1ASEJ, y1ASEJ, x1ASEJ - x0ASEJ, y1ASEJ - y0ASEJ, sASEJ);
    }

    private static void puntaASEJ(GraphicsContext gcASEJ, double xASEJ, double yASEJ, double dxASEJ, double dyASEJ, double sASEJ) {
        double largoASEJ = Math.hypot(dxASEJ, dyASEJ);
        if (largoASEJ == 0) {
            return;
        }
        double uxASEJ = dxASEJ / largoASEJ;
        double uyASEJ = dyASEJ / largoASEJ;
        double tASEJ = 0.12 * sASEJ;
        gcASEJ.fillPolygon(new double[]{xASEJ, xASEJ - uxASEJ * tASEJ - uyASEJ * tASEJ * 0.6, xASEJ - uxASEJ * tASEJ + uyASEJ * tASEJ * 0.6},
                new double[]{yASEJ, yASEJ - uyASEJ * tASEJ + uxASEJ * tASEJ * 0.6, yASEJ - uyASEJ * tASEJ - uxASEJ * tASEJ * 0.6}, 3);
    }

    private static void textoASEJ(GraphicsContext gcASEJ, String textoASEJ, double cxASEJ, double cyASEJ, double tamanoASEJ, boolean negritaASEJ, Color colorASEJ) {
        gcASEJ.setFont(Font.font("System", negritaASEJ ? FontWeight.BOLD : FontWeight.NORMAL, Math.max(6, tamanoASEJ)));
        gcASEJ.setFill(colorASEJ);
        gcASEJ.setTextAlign(TextAlignment.CENTER);
        gcASEJ.setTextBaseline(VPos.CENTER);
        gcASEJ.fillText(textoASEJ, cxASEJ, cyASEJ);
    }

    private static double medirASEJ(String textoASEJ, double tamanoASEJ, boolean negritaASEJ) {
        Text tASEJ = new Text(textoASEJ);
        tASEJ.setFont(Font.font("System", negritaASEJ ? FontWeight.BOLD : FontWeight.NORMAL, Math.max(6, tamanoASEJ)));
        return tASEJ.getLayoutBounds().getWidth();
    }

    public static Color colorTipoASEJ(TipoHallazgoDefASEJ tipoASEJ, PaletaOdontogramaASEJ paletaASEJ) {
        return paletaASEJ.colorASEJ(tipoASEJ.reglaColorASEJ() == ReglaColorASEJ.SIEMPRE_ROJO ? ColorHallazgoASEJ.ROJO : ColorHallazgoASEJ.AZUL);
    }
}
