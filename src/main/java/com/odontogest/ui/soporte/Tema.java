/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.soporte;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;

/**
 * Tema visual de AtlantaFX (Primer claro u oscuro). El valor elegido se guarda en la
 * configuración {@code ui.tema}.
 */
public enum Tema {
    CLARO, OSCURO;

    private static volatile Tema actual = CLARO;

    /** Aplica el tema a toda la aplicación. Debe llamarse en el hilo de UI. */
    public void aplicar() {
        Application.setUserAgentStylesheet(this == OSCURO
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
        actual = this;
    }

    public Tema alternar() {
        return this == CLARO ? OSCURO : CLARO;
    }

    public static Tema actual() {
        return actual;
    }

    /** Interpreta el valor guardado; cualquier valor desconocido equivale a {@link #CLARO}. */
    public static Tema desde(String valor) {
        return "OSCURO".equalsIgnoreCase(valor == null ? "" : valor.trim()) ? OSCURO : CLARO;
    }
}
