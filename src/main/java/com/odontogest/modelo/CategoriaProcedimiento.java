/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

/**
 * Categoría de un procedimiento del tarifario (columna {@code procedimiento.categoria}).
 */
public enum CategoriaProcedimiento {
    DIAGNOSTICO("Diagnóstico"), PREVENTIVO("Preventivo"), OPERATORIA("Operatoria"), ENDODONCIA("Endodoncia"),
    PERIODONCIA("Periodoncia"), CIRUGIA("Cirugía"), PROTESIS("Prótesis"), IMPLANTOLOGIA("Implantología"),
    ORTODONCIA("Ortodoncia"), ESTETICA("Estética");

    public final String etiqueta;

    CategoriaProcedimiento(String etiqueta) {
        this.etiqueta = etiqueta;
    }
}
