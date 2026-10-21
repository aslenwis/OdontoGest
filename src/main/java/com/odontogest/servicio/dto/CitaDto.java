/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.EstadoCita;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Cita de la agenda.
 *
 * @param colorAgenda       color del odontólogo (#RRGGBB)
 * @param planItemDescripcion procedimiento del plan asociado ({@code null} si no hay)
 * @param atencionId        atención abierta desde la cita ({@code null} si aún no se atiende)
 */
public record CitaDto(long id, long pacienteId, String pacienteNombre, String numeroHistoria, String pacienteTelefono,
                      long odontologoId, String odontologoNombre, String colorAgenda, LocalDateTime inicio,
                      LocalDateTime fin, String motivo, EstadoCita estado, Long planItemId, String planItemDescripcion,
                      String notas, Long atencionId) {

    public int duracionMin() {
        return (int) Duration.between(inicio, fin).toMinutes();
    }
}
