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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Detección de superficie por clic. Corona de prueba: x 100–200, y 50–150 (centro 150, 100; s = 100).
 * No levanta JavaFX: {@link Rectangle2D} es una clase de geometría sin toolkit.
 */
class DetectorSuperficieTest {

    private static final Rectangle2D CORONA = new Rectangle2D(100, 50, 100, 100);

    private static Optional<Superficie> clic(double x, double y, int fdi) {
        return DetectorSuperficie.detectar(x, y, CORONA, PiezaDental.de(fdi));
    }

    @ParameterizedTest
    @CsvSource({"16", "21", "36", "48", "55", "61", "73", "84"})
    void centroEsOclusal(int fdi) {
        assertThat(clic(150, 100, fdi)).contains(Superficie.O);
        assertThat(clic(170, 120, fdi)).as("borde del cuadrado central").contains(Superficie.O);
        assertThat(clic(130, 80, fdi)).contains(Superficie.O);
    }

    /**
     * Las cuatro regiones para una pieza de cada cuadrante.
     * Columnas: fdi, arriba, abajo, izquierda, derecha.
     */
    @ParameterizedTest
    @CsvSource({
            "16, V, L, D, M",      // C1: superior, lado derecho del paciente (dibujado a la izquierda)
            "21, V, L, M, D",      // C2: superior, lado izquierdo
            "36, L, V, M, D",      // C3: inferior, lado izquierdo
            "46, L, V, D, M",      // C4: inferior, lado derecho
            "54, V, L, D, M",      // C5
            "63, V, L, M, D",      // C6
            "75, L, V, M, D",      // C7
            "82, L, V, D, M"       // C8
    })
    void cuatroRegionesPorCuadrante(int fdi, Superficie arriba, Superficie abajo, Superficie izquierda, Superficie derecha) {
        assertThat(clic(150, 55, fdi)).as("arriba").contains(arriba);
        assertThat(clic(150, 145, fdi)).as("abajo").contains(abajo);
        assertThat(clic(105, 100, fdi)).as("izquierda").contains(izquierda);
        assertThat(clic(195, 100, fdi)).as("derecha").contains(derecha);
    }

    @Test
    void enLasDiagonalesExactasGanaLaRegionVertical() {
        assertThat(clic(110, 60, 16)).as("|dx| = |dy| arriba-izquierda").contains(Superficie.V);
        assertThat(clic(190, 140, 16)).as("abajo-derecha").contains(Superficie.L);
        assertThat(clic(110, 140, 36)).as("abajo-izquierda en inferior").contains(Superficie.V);
        assertThat(clic(190, 60, 36)).contains(Superficie.L);
    }

    @Test
    void justoFueraDeLaDiagonalEsRegionHorizontal() {
        assertThat(clic(109, 60.5, 16)).contains(Superficie.D);
        assertThat(clic(191, 60.5, 21)).contains(Superficie.D);
    }

    @Test
    void bordesExactosDeLaCoronaCuentanComoDentro() {
        assertThat(clic(100, 100, 16)).contains(Superficie.D);
        assertThat(clic(200, 100, 16)).contains(Superficie.M);
        assertThat(clic(150, 50, 16)).contains(Superficie.V);
        assertThat(clic(150, 150, 16)).contains(Superficie.L);
        assertThat(clic(100, 50, 16)).as("esquina").contains(Superficie.V);
    }

    @Test
    void fueraDeLaCoronaNoHaySuperficie() {
        assertThat(clic(99.9, 100, 16)).isEmpty();
        assertThat(clic(200.1, 100, 16)).isEmpty();
        assertThat(clic(150, 49.9, 16)).isEmpty();
        assertThat(clic(150, 150.1, 16)).isEmpty();
        assertThat(clic(0, 0, 16)).isEmpty();
    }
}
