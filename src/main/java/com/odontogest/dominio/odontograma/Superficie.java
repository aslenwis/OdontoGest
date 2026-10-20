/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

/**
 * Superficie de la corona dental con su bit en la máscara persistida (rango 0–31).
 * <p>
 * O se rotula "I" (incisal) en piezas anteriores y L se rotula "P" (palatino) en superiores;
 * ese rotulado depende de la pieza y se agrega en la Fase 1 junto con {@code PiezaDental}.
 */
public enum Superficie {
    O(1), M(2), D(4), V(8), L(16);

    public final int bit;

    Superficie(int bit) {
        this.bit = bit;
    }
}
