/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.Denticion;
import com.odontogest.dominio.odontograma.Superficie;
import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Geometría del odontograma (sin levantar JavaFX).
 */
class LayoutOdontogramaTest {

    private static final double ANCHO = 1200;
    private static final double ALTO = 700;

    @Test
    void cantidadDeCeldasPorDenticion() {
        assertThat(LayoutOdontograma.calcular(ANCHO, ALTO, Denticion.PERMANENTE).celdas()).hasSize(32);
        assertThat(LayoutOdontograma.calcular(ANCHO, ALTO, Denticion.DECIDUA).celdas()).hasSize(20);
        assertThat(LayoutOdontograma.calcular(ANCHO, ALTO, Denticion.MIXTA).celdas()).hasSize(52);
    }

    @Test
    void ladoSegunLaFormulaDelPlan() {
        LayoutOdontograma ancho = LayoutOdontograma.calcular(ANCHO, 2000, Denticion.PERMANENTE);
        double s = ancho.lado();
        assertThat(16 * s + 15 * 0.12 * s + 0.5 * s).as("limitado por el ancho")
                .isCloseTo(ANCHO - 2 * LayoutOdontograma.MARGEN, within(1e-6));

        LayoutOdontograma alto = LayoutOdontograma.calcular(3000, 400, Denticion.MIXTA);
        assertThat(alto.lado()).as("limitado por el alto de fila")
                .isCloseTo((400 - 2 * LayoutOdontograma.MARGEN) / 4 * LayoutOdontograma.FACTOR_ALTO_FILA, within(1e-6));
        assertThat(alto.separacion()).isCloseTo(0.12 * alto.lado(), within(1e-9));
    }

    @ParameterizedTest
    @EnumSource(Denticion.class)
    void lasCeldasNoSeSuperponenYQuedanDentroDelArea(Denticion denticion) {
        for (double[] area : new double[][]{{ANCHO, ALTO}, {800, 900}, {1600, 300}}) {
            LayoutOdontograma layout = LayoutOdontograma.calcular(area[0], area[1], denticion);
            List<Rectangle2D> rects = new ArrayList<>();
            for (CeldaPieza c : layout.celdas()) {
                Rectangle2D r = c.limites();
                assertThat(r.getMinX()).isGreaterThanOrEqualTo(0);
                assertThat(r.getMinY()).isGreaterThanOrEqualTo(0);
                assertThat(r.getMaxX()).isLessThanOrEqualTo(area[0]);
                assertThat(r.getMaxY()).isLessThanOrEqualTo(area[1]);
                rects.add(r);
            }
            for (int i = 0; i < rects.size(); i++) {
                for (int j = i + 1; j < rects.size(); j++) {
                    assertThat(rects.get(i).intersects(rects.get(j))).as("celdas %d y %d", i, j).isFalse();
                }
            }
        }
    }

    @Test
    void lineaMediaCentradaYPiezasEnOrdenDeDibujo() {
        LayoutOdontograma l = LayoutOdontograma.calcular(ANCHO, ALTO, Denticion.MIXTA);
        double centro = ANCHO / 2;
        assertThat(l.celda(11).corona().getMaxX()).isLessThan(centro);
        assertThat(l.celda(21).corona().getMinX()).isGreaterThan(centro);
        assertThat(centro - l.celda(11).corona().getMaxX())
                .isCloseTo(l.celda(21).corona().getMinX() - centro, within(1e-9));
        assertThat(centro - l.celda(51).corona().getMaxX())
                .as("deciduas sobre la misma línea media").isCloseTo(centro - l.celda(11).corona().getMaxX(), within(1e-9));
        assertThat(l.celda(18).corona().getMinX()).isLessThan(l.celda(17).corona().getMinX());
        assertThat(l.celda(48).corona().getMinX()).isLessThan(l.celda(41).corona().getMinX());
        assertThat(l.celda(31).corona().getMinX()).isLessThan(l.celda(38).corona().getMinX());
        assertThat(l.celda(55).corona().getMinY()).isGreaterThan(l.celda(18).corona().getMinY());
        assertThat(l.celda(48).corona().getMinY()).isGreaterThan(l.celda(85).corona().getMinY());
    }

    @Test
    void ordenDeLaNtsYArcadaInferiorEnEspejo() {
        LayoutOdontograma l = LayoutOdontograma.calcular(ANCHO, ALTO, Denticion.PERMANENTE);
        CeldaPieza sup = l.celda(16);
        assertThat(sup.etiqueta().getY()).as("número entre recuadro y raíz")
                .isGreaterThan(sup.recuadro().getMaxY()).isLessThan(sup.raiz().getMinY());
        assertThat(sup.raiz().getMaxY()).isCloseTo(sup.corona().getMinY(), within(1e-9));

        CeldaPieza inf = l.celda(46);
        assertThat(inf.corona().getMaxY()).isCloseTo(inf.raiz().getMinY(), within(1e-9));
        assertThat(inf.etiqueta().getY()).isGreaterThan(inf.raiz().getMaxY()).isLessThan(inf.recuadro().getMinY());
        assertThat(inf.corona().getWidth()).isEqualTo(inf.corona().getHeight()).isEqualTo(l.lado());

        assertThat(inf.corona().getMinY() - sup.corona().getMaxY())
                .as("margen oclusal libre entre las coronas de ambas arcadas").isGreaterThan(0.7 * l.lado());
    }

    @Test
    void celdaEnUnPuntoYDeteccionDeSuperficieIntegradas() {
        LayoutOdontograma l = LayoutOdontograma.calcular(ANCHO, ALTO, Denticion.PERMANENTE);
        Rectangle2D corona = l.celda(36).corona();
        double x = corona.getMinX() + 2;
        double y = corona.getMinY() + corona.getHeight() / 2;

        CeldaPieza celda = l.celdaEn(x, y).orElseThrow();

        assertThat(celda.pieza().fdi()).isEqualTo(36);
        assertThat(DetectorSuperficie.detectar(x, y, celda.corona(), celda.pieza()))
                .as("36 se dibuja a la derecha: su lado izquierdo es mesial").contains(Superficie.M);
        assertThat(l.celdaEn(1, 1)).isEmpty();
        assertThat(l.celdaEn(l.celda(36).recuadro().getMinX() + 1, l.celda(36).recuadro().getMinY() + 1))
                .map(c -> c.pieza().fdi()).contains(36);
    }

    @Test
    void piezaAjenaALaDenticion() {
        LayoutOdontograma l = LayoutOdontograma.calcular(ANCHO, ALTO, Denticion.PERMANENTE);
        assertThat(l.contiene(55)).isFalse();
        assertThat(l.contiene(11)).isTrue();
        assertThatThrownBy(() -> l.celda(55)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void areaSinTamanoNoFalla() {
        LayoutOdontograma l = LayoutOdontograma.calcular(0, 0, Denticion.MIXTA);
        assertThat(l.lado()).isPositive();
        assertThat(l.celdas()).hasSize(52);
    }
}
