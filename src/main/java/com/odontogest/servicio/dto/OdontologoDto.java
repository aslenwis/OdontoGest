/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.util.List;

/**
 * Odontólogo con su horario semanal.
 */
public record OdontologoDto(long id, String nombres, String apellidos, String cop, String rne, String especialidad,
                            String telefono, String email, String colorAgenda, boolean activo,
                            List<HorarioDto> horarios) {

    public OdontologoDto {
        horarios = List.copyOf(horarios);
    }

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }

    @Override
    public String toString() {
        return nombreCompleto();
    }
}
