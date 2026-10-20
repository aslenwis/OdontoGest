/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.util.List;

/**
 * Datos del formulario de odontólogo; {@code horarios} reemplaza por completo el horario anterior.
 */
public record OdontologoForm(Long id, String nombres, String apellidos, String cop, String rne, String especialidad,
                             String telefono, String email, String colorAgenda, boolean activo,
                             List<HorarioDto> horarios) {

    public OdontologoForm {
        horarios = horarios == null ? List.of() : List.copyOf(horarios);
    }
}
