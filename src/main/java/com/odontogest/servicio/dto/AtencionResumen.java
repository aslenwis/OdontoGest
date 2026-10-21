/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.EstadoAtencion;

import java.time.LocalDateTime;

/**
 * Fila de la línea de tiempo de atenciones del paciente.
 */
public record AtencionResumen(long id, LocalDateTime fecha, String odontologoNombre, String motivoConsulta,
                              EstadoAtencion estado) {
}
