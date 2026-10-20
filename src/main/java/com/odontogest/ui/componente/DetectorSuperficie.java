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

import java.util.Optional;

/**
 * Detección geométrica de la superficie dental bajo un clic.
 * <pre>
 * dx = x − cx ; dy = y − cy ; h = s/2 ; r = 0,2·s
 * |dx| &gt; h o |dy| &gt; h            → ninguna (fuera de la corona)
 * |dx| ≤ r y |dy| ≤ r              → O (oclusal/incisal)
 * |dy| ≥ |dx| (región vertical)    → arriba: V en arcada superior, L en inferior; abajo: al revés
 * si no (región horizontal)        → la mesial apunta a la línea media:
 *     lado derecho del paciente (dibujado a la izquierda): derecha → M, izquierda → D
 *     lado izquierdo del paciente (dibujado a la derecha): izquierda → M, derecha → D
 * </pre>
 * En las diagonales exactas ({@code |dy| = |dx|}) gana la región vertical.
 */
public final class DetectorSuperficie {

    static final double FACTOR_CENTRO = 0.2;

    private DetectorSuperficie() {
    }

    public static Optional<Superficie> detectar(double x, double y, Rectangle2D corona, PiezaDental pieza) {
        double s = corona.getWidth();
        double h = s / 2;
        double r = FACTOR_CENTRO * s;
        double dx = x - (corona.getMinX() + h);
        double dy = y - (corona.getMinY() + h);
        double adx = Math.abs(dx);
        double ady = Math.abs(dy);

        if (adx > h || ady > h) {
            return Optional.empty();
        }
        if (adx <= r && ady <= r) {
            return Optional.of(Superficie.O);
        }
        if (ady >= adx) {
            boolean superior = pieza.arcada() == Arcada.SUPERIOR;
            boolean arriba = dy < 0;
            return Optional.of(arriba == superior ? Superficie.V : Superficie.L);
        }
        boolean derecha = dx > 0;
        return Optional.of(derecha == pieza.ladoDerechoPaciente() ? Superficie.M : Superficie.D);
    }
}
