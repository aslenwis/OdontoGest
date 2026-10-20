/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

/**
 * Cuadrante del sistema FDI (dígito de las decenas del número de pieza).
 * <p>
 * 1–4 dentición permanente, 5–8 decidua. Los cuadrantes 1, 4, 5 y 8 son el lado derecho del paciente,
 * que el observador ve a su izquierda.
 */
public enum Cuadrante {
    C1(1, Arcada.SUPERIOR, true, false, 8),
    C2(2, Arcada.SUPERIOR, false, false, 8),
    C3(3, Arcada.INFERIOR, false, false, 8),
    C4(4, Arcada.INFERIOR, true, false, 8),
    C5(5, Arcada.SUPERIOR, true, true, 5),
    C6(6, Arcada.SUPERIOR, false, true, 5),
    C7(7, Arcada.INFERIOR, false, true, 5),
    C8(8, Arcada.INFERIOR, true, true, 5);

    public final int numero;
    public final Arcada arcada;
    public final boolean ladoDerechoPaciente;
    public final boolean deciduo;
    /** Número de piezas del cuadrante (posiciones válidas 1..piezas). */
    public final int piezas;

    Cuadrante(int numero, Arcada arcada, boolean ladoDerechoPaciente, boolean deciduo, int piezas) {
        this.numero = numero;
        this.arcada = arcada;
        this.ladoDerechoPaciente = ladoDerechoPaciente;
        this.deciduo = deciduo;
        this.piezas = piezas;
    }

    public static Cuadrante de(int numero) {
        if (numero < 1 || numero > 8) {
            throw new IllegalArgumentException("Cuadrante FDI inválido: " + numero);
        }
        return values()[numero - 1];
    }
}
