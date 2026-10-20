/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

/**
 * Clasificación de severidad de la OMS para el CPO-D (referencia: edad índice de 12 años).
 * <pre>
 * MUY_BAJO 0,0–1,1 · BAJO 1,2–2,6 · MODERADO 2,7–4,4 · ALTO 4,5–6,5 · MUY_ALTO ≥ 6,6
 * </pre>
 */
public enum NivelSeveridadOms {
    MUY_BAJO(1.2), BAJO(2.7), MODERADO(4.5), ALTO(6.6), MUY_ALTO(Double.POSITIVE_INFINITY);

    /** Límite superior exclusivo del nivel. */
    private final double limiteSuperior;

    NivelSeveridadOms(double limiteSuperior) {
        this.limiteSuperior = limiteSuperior;
    }

    /** Nivel para un valor individual o un promedio (los promedios se redondean a un decimal). */
    public static NivelSeveridadOms de(double valor) {
        if (valor < 0 || Double.isNaN(valor)) {
            throw new IllegalArgumentException("Valor de índice inválido: " + valor);
        }
        double redondeado = Math.round(valor * 10) / 10.0;
        for (NivelSeveridadOms nivel : values()) {
            if (redondeado < nivel.limiteSuperior) {
                return nivel;
            }
        }
        return MUY_ALTO;
    }
}
