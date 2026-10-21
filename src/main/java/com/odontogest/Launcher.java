/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest;

import com.odontogest.config.DirectorioDatos;
import com.odontogest.config.RestauracionPendiente;
import com.odontogest.ui.PantallaCarga;
import javafx.application.Application;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Punto de entrada del ejecutable.
 * <p>
 * No extiende {@link Application} a propósito: así el fat JAR arranca sin exigir
 * JavaFX en el module-path. Prepara la carpeta de datos y aplica una restauración pendiente antes de levantar
 * Spring.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        // Interfaz en español: nombres de meses y días en DatePicker, fechas largas en PDF.
        Locale.setDefault(Locale.forLanguageTag("es-PE"));
        Path home = DirectorioDatos.inicializar();
        // Un respaldo programado para restaurar reemplaza la base antes de que Spring la abra (PLAN §5.4).
        try {
            if (RestauracionPendiente.aplicarSiExiste(home)) {
                System.setProperty(RestauracionPendiente.PROPIEDAD_APLICADA, "true");
            }
        } catch (RuntimeException e) {
            // La base anterior sigue en su lugar; la marca queda para reintentar en el próximo arranque.
            LoggerFactory.getLogger(Launcher.class).error("No se pudo aplicar la restauración pendiente", e);
        }
        // Pantalla de carga mientras Spring arranca (y, la primera vez, se siembran los datos demo).
        System.setProperty("javafx.preloader", PantallaCarga.class.getName());
        Application.launch(JavaFxApplication.class, args);
    }
}
