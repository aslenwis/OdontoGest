/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

/**
 * Sexo registrado del paciente (columna {@code paciente.sexo}).
 */
public enum Sexo {
    M("Masculino"), F("Femenino");

    public final String etiqueta;

    Sexo(String etiqueta) {
        this.etiqueta = etiqueta;
    }
}
