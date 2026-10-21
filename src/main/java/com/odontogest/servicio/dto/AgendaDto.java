/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Agenda de un odontólogo para un rango de días (RF-AGE-01): su horario de atención y las citas que no están
 * canceladas.
 *
 * @param intervaloMin paso de la grilla (configuración {@code agenda.intervalo_min})
 */
public record AgendaDto(LocalDate desde, int dias, OdontologoDto odontologo, List<CitaDto> citas, int intervaloMin) {

    public AgendaDto {
        citas = List.copyOf(citas);
    }
}
