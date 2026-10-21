/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.ui.soporte;

import javafx.application.Platform;

/**
 * Lleva al hilo de JavaFX el trabajo de los oyentes de eventos de dominio, que llegan desde hilos de fondo.
 * <p>
 * Un oyente nunca debe hacer fallar al servicio que publicó el evento: si JavaFX no está iniciado (pruebas,
 * datos de demostración) el aviso simplemente se descarta.
 */
public final class HiloUi {

    private HiloUi() {
    }

    public static void ejecutar(Runnable trabajo) {
        if (Platform.isFxApplicationThread()) {
            trabajo.run();
            return;
        }
        try {
            Platform.runLater(trabajo);
        } catch (IllegalStateException sinJavaFx) {
            // Sin interfaz no hay nada que refrescar.
        }
    }
}
