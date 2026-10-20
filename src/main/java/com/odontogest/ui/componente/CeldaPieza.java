/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.PiezaDental;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

/**
 * Geometría de una pieza en el odontograma.
 *
 * @param corona   cuadrado dividido en las 5 superficies
 * @param raiz     zona radicular (encima de la corona en la arcada superior, debajo en la inferior)
 * @param recuadro recuadro donde se escribe la sigla
 * @param etiqueta centro donde se escribe el número FDI
 */
public record CeldaPieza(PiezaDental pieza, Rectangle2D corona, Rectangle2D raiz, Rectangle2D recuadro, Point2D etiqueta) {

    /** Rectángulo que envuelve recuadro, raíz y corona. */
    public Rectangle2D limites() {
        double minX = Math.min(corona.getMinX(), Math.min(raiz.getMinX(), recuadro.getMinX()));
        double minY = Math.min(corona.getMinY(), Math.min(raiz.getMinY(), recuadro.getMinY()));
        double maxX = Math.max(corona.getMaxX(), Math.max(raiz.getMaxX(), recuadro.getMaxX()));
        double maxY = Math.max(corona.getMaxY(), Math.max(raiz.getMaxY(), recuadro.getMaxY()));
        return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
    }

    public boolean contiene(double x, double y) {
        return limites().contains(x, y);
    }
}
