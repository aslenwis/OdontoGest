/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.odontograma;

/**
 * Dentición que muestra un odontograma: permanente (11–48), decidua (51–85) o mixta (ambas).
 */
public enum Denticion {
    PERMANENTE, DECIDUA, MIXTA;

    /** Dentición habitual para la edad: decidua antes de los 6 años, mixta de 6 a 12, permanente desde los 13. */
    public static Denticion sugerida(int edad) {
        if (edad < 6) {
            return DECIDUA;
        }
        return edad <= 12 ? MIXTA : PERMANENTE;
    }

    /** Indica si la pieza puede registrarse en un odontograma de esta dentición. */
    public boolean admite(PiezaDental pieza) {
        return switch (this) {
            case PERMANENTE -> !pieza.esDecidua();
            case DECIDUA -> pieza.esDecidua();
            case MIXTA -> true;
        };
    }
}
