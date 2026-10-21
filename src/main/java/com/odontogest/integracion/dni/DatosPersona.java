/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.integracion.dni;

/**
 * Nombres de una persona devueltos por la API de consulta de DNI.
 */
public record DatosPersona(String nombres, String apellidoPaterno, String apellidoMaterno) {
}
