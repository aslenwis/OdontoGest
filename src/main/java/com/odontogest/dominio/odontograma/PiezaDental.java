/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import java.util.ArrayList;
import java.util.List;

/**
 * Pieza dental identificada con el sistema dígito dos (FDI), p. ej. 36 = primer molar inferior izquierdo.
 * <p>
 * Válidas: cuadrantes 1–4 con posiciones 1–8 (permanentes) y cuadrantes 5–8 con posiciones 1–5 (deciduas).
 * El orden de dibujo sigue las filas del odontograma: permanentes superiores, deciduas superiores,
 * deciduas inferiores y permanentes inferiores, cada fila de la derecha del paciente a su izquierda.
 */
public record PiezaDental(int fdi) implements Comparable<PiezaDental> {

    private static final List<PiezaDental> PERMANENTES_SUPERIORES = fila(1, 2, 8);
    private static final List<PiezaDental> DECIDUAS_SUPERIORES = fila(5, 6, 5);
    private static final List<PiezaDental> DECIDUAS_INFERIORES = fila(8, 7, 5);
    private static final List<PiezaDental> PERMANENTES_INFERIORES = fila(4, 3, 8);

    private static final List<PiezaDental> PERMANENTES = concatenar(PERMANENTES_SUPERIORES, PERMANENTES_INFERIORES);
    private static final List<PiezaDental> DECIDUAS = concatenar(DECIDUAS_SUPERIORES, DECIDUAS_INFERIORES);
    private static final List<PiezaDental> TODAS = concatenar(
            concatenar(PERMANENTES_SUPERIORES, DECIDUAS_SUPERIORES),
            concatenar(DECIDUAS_INFERIORES, PERMANENTES_INFERIORES));

    public PiezaDental {
        if (!esValida(fdi)) {
            throw new IllegalArgumentException("Pieza FDI inválida: " + fdi);
        }
    }

    public static PiezaDental de(int fdi) {
        return new PiezaDental(fdi);
    }

    public static boolean esValida(int fdi) {
        int cuadrante = fdi / 10;
        int posicion = fdi % 10;
        if (cuadrante >= 1 && cuadrante <= 4) {
            return posicion >= 1 && posicion <= 8;
        }
        if (cuadrante >= 5 && cuadrante <= 8) {
            return posicion >= 1 && posicion <= 5;
        }
        return false;
    }

    public Cuadrante cuadrante() {
        return Cuadrante.de(fdi / 10);
    }

    /** 1 = incisivo central … 8 = tercera molar. */
    public int posicion() {
        return fdi % 10;
    }

    public boolean esDecidua() {
        return cuadrante().deciduo;
    }

    public Arcada arcada() {
        return cuadrante().arcada;
    }

    /** Lado derecho del paciente (cuadrantes 1, 4, 5, 8): se dibuja a la izquierda del observador. */
    public boolean ladoDerechoPaciente() {
        return cuadrante().ladoDerechoPaciente;
    }

    /** Incisivos y caninos. */
    public boolean esAnterior() {
        return posicion() <= 3;
    }

    public TipoPieza tipo() {
        int p = posicion();
        if (p <= 2) {
            return TipoPieza.INCISIVO;
        }
        if (p == 3) {
            return TipoPieza.CANINO;
        }
        if (p <= 5) {
            return esDecidua() ? TipoPieza.MOLAR : TipoPieza.PREMOLAR;
        }
        return TipoPieza.MOLAR;
    }

    /**
     * Simplificación clínica: molares multirradiculares; primeros premolares superiores (14, 24)
     * birradiculares; el resto unirradiculares.
     */
    public TipoRadicular tipoRadicular() {
        if (tipo() == TipoPieza.MOLAR) {
            return TipoRadicular.MULTIRRADICULAR;
        }
        if (fdi == 14 || fdi == 24) {
            return TipoRadicular.BIRRADICULAR;
        }
        return TipoRadicular.UNIRRADICULAR;
    }

    public boolean esTerceraMolar() {
        return posicion() == 8;
    }

    /** Índice global en el orden de dibujo (0–51), útil para ordenar listados. */
    public int ordenDibujo() {
        return TODAS.indexOf(this);
    }

    /** Dos piezas vecinas en la misma fila de dibujo (incluye 11–21, contiguas sobre la línea media). */
    public boolean esContiguaA(PiezaDental otra) {
        List<PiezaDental> fila = filaDe(this);
        int i = fila.indexOf(this);
        int j = fila.indexOf(otra);
        return j >= 0 && Math.abs(i - j) == 1;
    }

    public boolean mismaArcada(PiezaDental otra) {
        return arcada() == otra.arcada();
    }

    /** Permanentes en orden de dibujo: 18..11, 21..28, 48..41, 31..38. */
    public static List<PiezaDental> permanentes() {
        return PERMANENTES;
    }

    /** Deciduas en orden de dibujo: 55..51, 61..65, 85..81, 71..75. */
    public static List<PiezaDental> deciduas() {
        return DECIDUAS;
    }

    /** Filas de dibujo (de arriba hacia abajo) de la dentición indicada. */
    public static List<List<PiezaDental>> filas(Denticion denticion) {
        return switch (denticion) {
            case PERMANENTE -> List.of(PERMANENTES_SUPERIORES, PERMANENTES_INFERIORES);
            case DECIDUA -> List.of(DECIDUAS_SUPERIORES, DECIDUAS_INFERIORES);
            case MIXTA -> List.of(PERMANENTES_SUPERIORES, DECIDUAS_SUPERIORES, DECIDUAS_INFERIORES,
                    PERMANENTES_INFERIORES);
        };
    }

    @Override
    public int compareTo(PiezaDental otra) {
        return Integer.compare(ordenDibujo(), otra.ordenDibujo());
    }

    @Override
    public String toString() {
        return String.valueOf(fdi);
    }

    private static List<PiezaDental> filaDe(PiezaDental p) {
        return switch (p.cuadrante()) {
            case C1, C2 -> PERMANENTES_SUPERIORES;
            case C5, C6 -> DECIDUAS_SUPERIORES;
            case C8, C7 -> DECIDUAS_INFERIORES;
            case C4, C3 -> PERMANENTES_INFERIORES;
        };
    }

    /** Fila: cuadrante derecho del paciente de distal a mesial, luego el izquierdo de mesial a distal. */
    private static List<PiezaDental> fila(int cuadranteDerecho, int cuadranteIzquierdo, int piezas) {
        List<PiezaDental> fila = new ArrayList<>();
        for (int p = piezas; p >= 1; p--) {
            fila.add(new PiezaDental(cuadranteDerecho * 10 + p));
        }
        for (int p = 1; p <= piezas; p++) {
            fila.add(new PiezaDental(cuadranteIzquierdo * 10 + p));
        }
        return List.copyOf(fila);
    }

    private static List<PiezaDental> concatenar(List<PiezaDental> a, List<PiezaDental> b) {
        List<PiezaDental> r = new ArrayList<>(a);
        r.addAll(b);
        return List.copyOf(r);
    }
}
