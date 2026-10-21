/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

/**
 * Nombres obtenidos de la consulta de DNI para completar el formulario del paciente (RF-PAC-06).
 */
public record PersonaDniDto(String nombres, String apellidoPaterno, String apellidoMaterno) {
}
