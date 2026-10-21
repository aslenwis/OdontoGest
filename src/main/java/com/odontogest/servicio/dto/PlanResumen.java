/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.EstadoPlan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Plan de tratamiento en la lista de planes del paciente.
 */
public record PlanResumen(long id, LocalDateTime fecha, EstadoPlan estado, BigDecimal total, String odontologoNombre) {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Por ejemplo "Plan N° 12 · 05/10/2026 · Propuesto". */
    public String etiqueta() {
        return "Plan N° " + id + " · " + FECHA.format(fecha) + " · " + estado.etiqueta();
    }

    @Override
    public String toString() {
        return etiqueta();
    }
}
