/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.Arcada;
import com.odontogest.dominio.odontograma.PiezaDental;
import com.odontogest.dominio.odontograma.Superficie;
import javafx.geometry.Rectangle2D;

/**
 * Corona dividida en 5 polígonos: cuadrado exterior y cuadrado interior centrado de lado 0,4·s.
 * <pre>
 * arriba (TL, TR, tr, tl) · derecha (TR, BR, br, tr) · abajo (BR, BL, bl, br) · izquierda (BL, TL, tl, bl)
 * centro (tl, tr, br, bl) = O/I
 * </pre>
 * Qué superficie ocupa cada región depende de la pieza, igual que en {@link DetectorSuperficie}: la vestibular
 * mira al borde exterior del odontograma y la mesial a la línea media. Solo usa {@code double}: se prueba sin JavaFX.
 */
public final class GeometriaCorona {

    /** Lado del cuadrado interior respecto de la corona (coincide con el radio 0,2·s del detector). */
    public static final double FACTOR_INTERIOR = 0.4;

    /** Región de la corona en pantalla. */
    public enum Region {
        ARRIBA, DERECHA, ABAJO, IZQUIERDA, CENTRO
    }

    private GeometriaCorona() {
    }

    public static Region region(PiezaDental pieza, Superficie s) {
        boolean superior = pieza.arcada() == Arcada.SUPERIOR;
        boolean derechoPaciente = pieza.ladoDerechoPaciente();
        return switch (s) {
            case O -> Region.CENTRO;
            case V -> superior ? Region.ARRIBA : Region.ABAJO;
            case L -> superior ? Region.ABAJO : Region.ARRIBA;
            case M -> derechoPaciente ? Region.DERECHA : Region.IZQUIERDA;
            case D -> derechoPaciente ? Region.IZQUIERDA : Region.DERECHA;
        };
    }

    /** Polígono de la superficie: {@code [0]} = coordenadas x, {@code [1]} = coordenadas y. */
    public static double[][] poligono(Rectangle2D corona, PiezaDental pieza, Superficie s) {
        return poligono(corona, region(pieza, s));
    }

    public static double[][] poligono(Rectangle2D c, Region region) {
        double x0 = c.getMinX();
        double y0 = c.getMinY();
        double x1 = c.getMaxX();
        double y1 = c.getMaxY();
        double m = (1 - FACTOR_INTERIOR) / 2 * c.getWidth();
        double ix0 = x0 + m;
        double iy0 = y0 + m;
        double ix1 = x1 - m;
        double iy1 = y1 - m;
        return switch (region) {
            case ARRIBA -> new double[][]{{x0, x1, ix1, ix0}, {y0, y0, iy0, iy0}};
            case DERECHA -> new double[][]{{x1, x1, ix1, ix1}, {y0, y1, iy1, iy0}};
            case ABAJO -> new double[][]{{x1, x0, ix0, ix1}, {y1, y1, iy1, iy1}};
            case IZQUIERDA -> new double[][]{{x0, x0, ix0, ix0}, {y1, y0, iy0, iy1}};
            case CENTRO -> new double[][]{{ix0, ix1, ix1, ix0}, {iy0, iy0, iy1, iy1}};
        };
    }

    /** Centroide del polígono (para rótulos y pruebas). */
    public static double[] centro(double[][] poligono) {
        double cx = 0;
        double cy = 0;
        for (int i = 0; i < poligono[0].length; i++) {
            cx += poligono[0][i];
            cy += poligono[1][i];
        }
        return new double[]{cx / poligono[0].length, cy / poligono[1].length};
    }

    /** Borde exterior de la superficie {x1, y1, x2, y2}; para O es la línea media horizontal del cuadrado interior. */
    public static double[] bordeExterior(Rectangle2D c, PiezaDental pieza, Superficie s) {
        double[][] p = poligono(c, region(pieza, s));
        if (region(pieza, s) == Region.CENTRO) {
            double y = (p[1][0] + p[1][2]) / 2;
            return new double[]{p[0][0], y, p[0][1], y};
        }
        return new double[]{p[0][0], p[1][0], p[0][1], p[1][1]};
    }
}
