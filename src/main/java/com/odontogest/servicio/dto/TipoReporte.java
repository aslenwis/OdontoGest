/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

/**
 * Reportes disponibles en la pantalla de reportes; {@code ReporteService.disponibles()} indica cuáles puede ver
 * el usuario según su rol.
 */
public enum TipoReporte {
    INGRESOS("Ingresos"),
    PRODUCCION("Producción por odontólogo"),
    MOROSIDAD("Morosidad"),
    EPIDEMIOLOGICO("Epidemiológico"),
    AUDITORIA("Auditoría");

    public final String etiqueta;

    TipoReporte(String etiqueta) {
        this.etiqueta = etiqueta;
    }
}
