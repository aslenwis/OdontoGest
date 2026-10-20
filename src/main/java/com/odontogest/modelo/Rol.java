/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

/**
 * Rol de un usuario del sistema (columna {@code usuario.rol}).
 */
public enum Rol {
    ADMIN("Administrador"), ODONTOLOGO("Odontólogo"), RECEPCION("Recepción");

    public final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }
}
