/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.evento;

/**
 * Evento publicado al cerrar (firmar) un odontograma. Se publica desde hilos de fondo: los oyentes de UI
 * deben usar {@code Platform.runLater}.
 */
public record OdontogramaCerrado(long pacienteId, long odontogramaId, String hashCierre) {
}
