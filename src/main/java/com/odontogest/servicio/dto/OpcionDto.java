/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

/**
 * Opción de una lista fija (clave interna y texto visible).
 */
public record OpcionDto(String clave, String etiqueta) {

    @Override
    public String toString() {
        return etiqueta;
    }
}
