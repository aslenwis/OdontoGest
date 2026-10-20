/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

/**
 * Superficie de la corona dental con su bit en la máscara persistida (rango 0–31).
 * <p>
 * O se rotula "I" (incisal) en piezas anteriores y L se rotula "P" (palatino) en piezas superiores.
 */
public enum Superficie {
    O(1), M(2), D(4), V(8), L(16);

    public final int bit;

    Superficie(int bit) {
        this.bit = bit;
    }

    /** Sigla visible para la pieza: O/I, M, D, V, L/P. */
    public String etiqueta(PiezaDental pieza) {
        return switch (this) {
            case O -> pieza.esAnterior() ? "I" : "O";
            case L -> pieza.arcada() == Arcada.SUPERIOR ? "P" : "L";
            default -> name();
        };
    }

    /** Nombre completo para la pieza, p. ej. "Oclusal", "Incisal", "Palatino". */
    public String nombre(PiezaDental pieza) {
        return switch (this) {
            case O -> pieza.esAnterior() ? "Incisal" : "Oclusal";
            case M -> "Mesial";
            case D -> "Distal";
            case V -> "Vestibular";
            case L -> pieza.arcada() == Arcada.SUPERIOR ? "Palatino" : "Lingual";
        };
    }
}
