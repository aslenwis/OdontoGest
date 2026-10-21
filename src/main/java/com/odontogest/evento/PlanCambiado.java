/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.evento;

/**
 * Lo publica la interfaz (en el hilo de JavaFX) cuando cambia un plan de tratamiento o se marca un procedimiento en
 * una atención, para que las pestañas Plan y Atenciones de la ficha se refresquen.
 */
public record PlanCambiado(long pacienteId) {
}
