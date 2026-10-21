/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

/**
 * Tipo de odontograma (columna {@code odontograma.tipo}): uno INICIAL por paciente y los de EVOLUCION.
 */
public enum TipoOdontograma {
    INICIAL("Inicial"), EVOLUCION("Evolución");

    public final String etiqueta;

    TipoOdontograma(String etiqueta) {
        this.etiqueta = etiqueta;
    }
}
