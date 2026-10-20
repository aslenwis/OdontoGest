/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Bloque del horario de atención de un odontólogo.
 */
public record HorarioDto(DayOfWeek dia, LocalTime inicio, LocalTime fin) {
}
