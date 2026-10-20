/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.Sexo;
import com.odontogest.modelo.TipoDocumento;

import java.time.LocalDate;

/**
 * Datos que envía el formulario de paciente.
 *
 * @param id {@code null} para registrar un paciente nuevo
 */
public record PacienteForm(
        Long id, TipoDocumento tipoDocumento, String numeroDocumento,
        String nombres, String apellidoPaterno, String apellidoMaterno,
        LocalDate fechaNacimiento, Sexo sexo, String telefono, String email, String direccion, String ocupacion,
        String contactoEmergenciaNombre, String contactoEmergenciaTelefono,
        String apoderadoNombre, String apoderadoDocumento, String apoderadoParentesco,
        boolean aceptaRecordatorios) {
}
