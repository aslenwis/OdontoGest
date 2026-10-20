/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

/**
 * Fila del listado de pacientes.
 *
 * @param documento tipo y número, p. ej. "DNI 71234567"
 */
public record PacienteResumen(long id, String numeroHistoria, String documento, String nombreCompleto,
                              int edad, String telefono, boolean activo) {
}
