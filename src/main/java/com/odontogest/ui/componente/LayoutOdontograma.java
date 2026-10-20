/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.Arcada;
import com.odontogest.dominio.odontograma.Denticion;
import com.odontogest.dominio.odontograma.PiezaDental;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Geometría del odontograma: una celda por pieza según el área disponible.
 * <p>
 * Filas (de arriba a abajo): permanentes superiores, deciduas superiores, deciduas inferiores y permanentes
 * inferiores; se omiten las que no corresponden a la dentición. Lado de la corona:
 * <pre>
 * s = min((ancho − 2·margen − 15·g − gLinea) / 16, altoFila · 0,45)   con g = 0,12·s y gLinea = 0,5·s
 * </pre>
 * Celda superior, de arriba a abajo: recuadro de sigla (0,35·s), raíz (0,6·s), corona (s) y número. En la
 * arcada inferior el orden se invierte (espejo), así la cara vestibular queda hacia el borde exterior.
 * Las filas deciduas (5 piezas por lado) se centran sobre la misma línea media.
 * <p>
 * Usa solo clases de geometría de JavaFX (sin nodos), por eso se prueba sin levantar la interfaz.
 */
public final class LayoutOdontograma {

    public static final double MARGEN = 16;
    static final double FACTOR_ALTO_FILA = 0.45;
    static final double SEPARACION = 0.12;
    static final double LINEA_MEDIA = 0.5;
    static final double ALTO_RECUADRO = 0.35;
    static final double ALTO_RAIZ = 0.6;
    /** Con 0,25 la celda mide 2,2·s ≤ altoFila (porque s ≤ 0,45·altoFila): las filas nunca se superponen. */
    static final double ALTO_ETIQUETA = 0.25;
    private static final double LADO_MINIMO = 1;

    private final double lado;
    private final Map<Integer, CeldaPieza> celdas;

    private LayoutOdontograma(double lado, Map<Integer, CeldaPieza> celdas) {
        this.lado = lado;
        this.celdas = Collections.unmodifiableMap(celdas);
    }

    public static LayoutOdontograma calcular(double ancho, double alto, Denticion denticion) {
        List<List<PiezaDental>> filas = PiezaDental.filas(denticion);
        double altoFila = Math.max(0, alto - 2 * MARGEN) / filas.size();
        double porAncho = Math.max(0, ancho - 2 * MARGEN) / (16 + 15 * SEPARACION + LINEA_MEDIA);
        double s = Math.max(LADO_MINIMO, Math.min(porAncho, altoFila * FACTOR_ALTO_FILA));
        double g = SEPARACION * s;
        double hueco = g + LINEA_MEDIA * s;
        double centroX = ancho / 2;
        double altoContenido = (ALTO_RECUADRO + ALTO_RAIZ + 1 + ALTO_ETIQUETA) * s;

        Map<Integer, CeldaPieza> celdas = new LinkedHashMap<>();
        for (int r = 0; r < filas.size(); r++) {
            List<PiezaDental> fila = filas.get(r);
            int porLado = fila.size() / 2;
            double y0 = MARGEN + r * altoFila + (altoFila - altoContenido) / 2;
            boolean superior = fila.get(0).arcada() == Arcada.SUPERIOR;
            for (int i = 0; i < fila.size(); i++) {
                double x = i < porLado
                        ? centroX - hueco / 2 - (porLado - i) * s - (porLado - 1 - i) * g
                        : centroX + hueco / 2 + (i - porLado) * (s + g);
                PiezaDental pieza = fila.get(i);
                celdas.put(pieza.fdi(), superior ? celdaSuperior(pieza, x, y0, s) : celdaInferior(pieza, x, y0, s));
            }
        }
        return new LayoutOdontograma(s, celdas);
    }

    private static CeldaPieza celdaSuperior(PiezaDental pieza, double x, double y0, double s) {
        Rectangle2D recuadro = new Rectangle2D(x, y0, s, ALTO_RECUADRO * s);
        Rectangle2D raiz = new Rectangle2D(x, y0 + ALTO_RECUADRO * s, s, ALTO_RAIZ * s);
        Rectangle2D corona = new Rectangle2D(x, y0 + (ALTO_RECUADRO + ALTO_RAIZ) * s, s, s);
        Point2D etiqueta = new Point2D(x + s / 2, corona.getMaxY() + ALTO_ETIQUETA * s / 2);
        return new CeldaPieza(pieza, corona, raiz, recuadro, etiqueta);
    }

    private static CeldaPieza celdaInferior(PiezaDental pieza, double x, double y0, double s) {
        Point2D etiqueta = new Point2D(x + s / 2, y0 + ALTO_ETIQUETA * s / 2);
        Rectangle2D corona = new Rectangle2D(x, y0 + ALTO_ETIQUETA * s, s, s);
        Rectangle2D raiz = new Rectangle2D(x, corona.getMaxY(), s, ALTO_RAIZ * s);
        Rectangle2D recuadro = new Rectangle2D(x, raiz.getMaxY(), s, ALTO_RECUADRO * s);
        return new CeldaPieza(pieza, corona, raiz, recuadro, etiqueta);
    }

    /** Celda bajo el punto (búsqueda lineal: como máximo 52 celdas). */
    public Optional<CeldaPieza> celdaEn(double x, double y) {
        for (CeldaPieza c : celdas.values()) {
            if (c.contiene(x, y)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    public CeldaPieza celda(int fdi) {
        CeldaPieza c = celdas.get(fdi);
        if (c == null) {
            throw new IllegalArgumentException("La pieza " + fdi + " no está en este odontograma");
        }
        return c;
    }

    public boolean contiene(int fdi) {
        return celdas.containsKey(fdi);
    }

    public Collection<CeldaPieza> celdas() {
        return celdas.values();
    }

    /** Lado {@code s} de la corona. */
    public double lado() {
        return lado;
    }

    /** Separación {@code g} entre celdas vecinas. */
    public double separacion() {
        return SEPARACION * lado;
    }
}
