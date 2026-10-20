/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.finanzas;

import java.time.LocalDate;

/**
 * Frecuencia de vencimiento de las cuotas de un cronograma.
 */
public enum Frecuencia {
    SEMANAL, QUINCENAL, MENSUAL;

    /**
     * Vencimiento de la cuota {@code i} (1 = primera). MENSUAL usa {@code plusMonths}: un día 31 pasa al
     * último día de los meses más cortos.
     */
    public LocalDate vencimiento(LocalDate primerVencimiento, int i) {
        return switch (this) {
            case SEMANAL -> primerVencimiento.plusWeeks(i - 1L);
            case QUINCENAL -> primerVencimiento.plusDays(15L * (i - 1));
            case MENSUAL -> primerVencimiento.plusMonths(i - 1L);
        };
    }
}
