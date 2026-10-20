/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.tratamiento;

/**
 * Unidad de cobro de un procedimiento del tarifario (columna {@code procedimiento.forma_cobro}).
 */
public enum FormaCobro {
    PIEZA, SUPERFICIE, ARCADA, BOCA, SESION
}
