/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

import java.util.Collection;
import java.util.EnumSet;

/**
 * Conversión entre conjuntos de {@link Superficie} y la máscara de bits {@code O=1, M=2, D=4, V=8, L=16}.
 */
public final class Superficies {

    public static final int MASCARA_MAXIMA = 31;

    private Superficies() {
    }

    public static int aMascara(Collection<Superficie> superficies) {
        int mascara = 0;
        if (superficies != null) {
            for (Superficie s : superficies) {
                mascara |= s.bit;
            }
        }
        return mascara;
    }

    public static EnumSet<Superficie> desdeMascara(int mascara) {
        if (mascara < 0 || mascara > MASCARA_MAXIMA) {
            throw new IllegalArgumentException("Máscara de superficies fuera de rango: " + mascara);
        }
        EnumSet<Superficie> resultado = EnumSet.noneOf(Superficie.class);
        for (Superficie s : Superficie.values()) {
            if ((mascara & s.bit) != 0) {
                resultado.add(s);
            }
        }
        return resultado;
    }
}
