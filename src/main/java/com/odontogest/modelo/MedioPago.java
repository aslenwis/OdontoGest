/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

/**
 * Medio de pago (columna {@code pago.medio}).
 */
public enum MedioPago {
    EFECTIVO("Efectivo", false),
    TARJETA("Tarjeta", true),
    YAPE("Yape", true),
    PLIN("Plin", true),
    TRANSFERENCIA("Transferencia", true);

    private final String etiqueta;
    private final boolean pideReferencia;

    MedioPago(String etiqueta, boolean pideReferencia) {
        this.etiqueta = etiqueta;
        this.pideReferencia = pideReferencia;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Medios electrónicos: se sugiere anotar el número de operación. */
    public boolean pideReferencia() {
        return pideReferencia;
    }
}
