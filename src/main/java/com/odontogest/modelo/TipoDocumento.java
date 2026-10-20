/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

/**
 * Tipo de documento de identidad del paciente (columna {@code paciente.tipo_documento}).
 */
public enum TipoDocumento {
    DNI("DNI"), CE("Carné de extranjería"), PASAPORTE("Pasaporte"), SIN_DOCUMENTO("Sin documento");

    public final String etiqueta;

    TipoDocumento(String etiqueta) {
        this.etiqueta = etiqueta;
    }
}
