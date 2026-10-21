/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

/**
 * Estado de registro de un odontograma (columna {@code odontograma.estado}): en BORRADOR se registran y
 * eliminan hallazgos; CERRADO (firmado) es inmutable y solo admite anulaciones.
 */
public enum EstadoOdontogramaRegistro {
    BORRADOR, CERRADO
}
