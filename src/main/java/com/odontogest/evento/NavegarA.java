/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.evento;

import java.util.Map;

/**
 * Pide a la ventana principal mostrar una vista, p. ej.
 * {@code new NavegarA("paciente-ficha-view", Map.of("pacienteId", 15L))}.
 *
 * @param vista nombre del FXML sin extensión
 */
public record NavegarA(String vista, Map<String, Object> parametros) {

    public NavegarA {
        parametros = parametros == null ? Map.of() : Map.copyOf(parametros);
    }

    public NavegarA(String vista) {
        this(vista, Map.of());
    }
}
