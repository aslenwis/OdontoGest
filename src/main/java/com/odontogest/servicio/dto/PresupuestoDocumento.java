/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

/**
 * Datos del PDF de presupuesto y del consentimiento informado de tratamiento.
 */
public record PresupuestoDocumento(DatosClinica clinica, PacienteDetalle paciente, PlanDto plan) {
}
