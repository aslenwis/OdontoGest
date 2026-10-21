/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.evento;

/**
 * Evento publicado por {@code CitaService} (y al cerrar una atención abierta desde una cita) cuando una cita se crea,
 * se mueve o cambia de estado. Se publica desde hilos de fondo: los oyentes de UI deben usar {@code HiloUi}.
 */
public record CitaCambiada(long citaId, long pacienteId, long odontologoId) {
}
