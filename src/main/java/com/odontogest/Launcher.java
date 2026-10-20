/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest;

import com.odontogest.config.DirectorioDatos;
import javafx.application.Application;

/**
 * Punto de entrada del ejecutable.
 * <p>
 * No extiende {@link Application} a propósito: así el fat JAR arranca sin exigir
 * JavaFX en el module-path. Prepara la carpeta de datos antes de levantar Spring.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        DirectorioDatos.inicializar();
        // Fase 8: RestauracionPendiente.aplicarSiExiste(home) antes de abrir la BD.
        Application.launch(JavaFxApplication.class, args);
    }
}
