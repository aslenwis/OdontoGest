/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.componente;

import com.odontogest.dominio.odontograma.PiezaDental;
import com.odontogest.dominio.odontograma.Superficie;
import javafx.geometry.Rectangle2D;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Lo que se dibuja en cada superficie coincide con lo que detecta el clic, para las 52 piezas.
 */
class GeometriaCoronaTest {

    private static final Rectangle2D CORONA = new Rectangle2D(100, 50, 100, 100);

    @Test
    void elCentroDeCadaPoligonoSeDetectaComoSuSuperficie() {
        for (PiezaDental p : PiezaDental.filas(com.odontogest.dominio.odontograma.Denticion.MIXTA).stream()
                .flatMap(java.util.List::stream).toList()) {
            for (Superficie s : Superficie.values()) {
                double[] c = GeometriaCorona.centro(GeometriaCorona.poligono(CORONA, p, s));
                assertThat(DetectorSuperficie.detectar(c[0], c[1], CORONA, p)).as("pieza %s, superficie %s", p, s).contains(s);
            }
        }
    }

    @Test
    void regionesSegunArcadaYLado() {
        assertThat(GeometriaCorona.region(PiezaDental.de(16), Superficie.V)).isEqualTo(GeometriaCorona.Region.ARRIBA);
        assertThat(GeometriaCorona.region(PiezaDental.de(46), Superficie.V)).isEqualTo(GeometriaCorona.Region.ABAJO);
        assertThat(GeometriaCorona.region(PiezaDental.de(16), Superficie.M)).isEqualTo(GeometriaCorona.Region.DERECHA);
        assertThat(GeometriaCorona.region(PiezaDental.de(26), Superficie.M)).isEqualTo(GeometriaCorona.Region.IZQUIERDA);
        assertThat(GeometriaCorona.region(PiezaDental.de(36), Superficie.O)).isEqualTo(GeometriaCorona.Region.CENTRO);
    }

    @Test
    void elCuadradoInteriorMideCuatroDecimosDelLado() {
        double[][] centro = GeometriaCorona.poligono(CORONA, GeometriaCorona.Region.CENTRO);
        assertThat(centro[0][1] - centro[0][0]).isCloseTo(40, within(1e-9));
        assertThat(centro[1][2] - centro[1][0]).isCloseTo(40, within(1e-9));
    }

    @Test
    void bordeExteriorDeLaSuperficie() {
        assertThat(GeometriaCorona.bordeExterior(CORONA, PiezaDental.de(11), Superficie.V)).containsExactly(100, 50, 200, 50);
        assertThat(GeometriaCorona.bordeExterior(CORONA, PiezaDental.de(41), Superficie.V)).containsExactly(200, 150, 100, 150);
        assertThat(GeometriaCorona.bordeExterior(CORONA, PiezaDental.de(11), Superficie.O)[1]).isCloseTo(100, within(1e-9));
    }
}
