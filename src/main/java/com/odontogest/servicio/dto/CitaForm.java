/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.time.LocalDateTime;

/**
 * Alta o edición de una cita (RF-AGE-02).
 *
 * @param id         {@code null} = cita nueva
 * @param planItemId procedimiento del plan que se hará (opcional)
 */
public record CitaForm(Long id, Long pacienteId, Long odontologoId, LocalDateTime inicio, int duracionMin, String motivo,
                       String notas, Long planItemId) {
}
