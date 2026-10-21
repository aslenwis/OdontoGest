/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.evento;

/**
 * La conexión a internet se perdió o se recuperó (RNF-03). Se publica desde un hilo de fondo: los oyentes de UI
 * deben usar {@code HiloUi}.
 */
public record ConectividadCambiada(boolean enLinea) {
}
