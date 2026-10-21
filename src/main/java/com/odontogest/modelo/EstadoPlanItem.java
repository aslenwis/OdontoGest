/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

/**
 * Estado de ejecución de un ítem del plan (columna {@code plan_item.estado}).
 */
public enum EstadoPlanItem {
    PENDIENTE("Pendiente"),
    EN_PROCESO("En proceso"),
    REALIZADO("Realizado"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoPlanItem(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }
}
