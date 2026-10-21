/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

/**
 * Procedimiento pendiente de un plan aceptado o en curso, para asociarlo a una cita.
 */
public record ItemPlanPendienteDto(long itemId, long planId, String descripcion) {

    @Override
    public String toString() {
        return "Plan N° " + planId + " · " + descripcion;
    }
}
