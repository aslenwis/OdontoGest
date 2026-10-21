/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.Sexo;
import com.odontogest.modelo.TipoDocumento;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ficha del paciente: datos personales más la alerta de alergias y el consentimiento de datos.
 *
 * @param alergias              de la anamnesis; si no está vacío la ficha muestra una alerta roja
 * @param consentimientoDatosEn último consentimiento de datos personales ({@code null} si falta)
 */
public record PacienteDetalle(
        long id, String numeroHistoria, TipoDocumento tipoDocumento, String numeroDocumento,
        String nombres, String apellidoPaterno, String apellidoMaterno,
        LocalDate fechaNacimiento, Sexo sexo, String telefono, String email, String direccion, String ocupacion,
        String contactoEmergenciaNombre, String contactoEmergenciaTelefono,
        String apoderadoNombre, String apoderadoDocumento, String apoderadoParentesco,
        boolean aceptaRecordatorios, boolean activo, int edad, String alergias, LocalDateTime consentimientoDatosEn,
        boolean demo) {

    public static final int MAYORIA_DE_EDAD = 18;

    /** "Nombres Apellido Apellido". */
    public String nombreCompleto() {
        return nombres + " " + apellidoPaterno + (apellidoMaterno == null ? "" : " " + apellidoMaterno);
    }

    public String documento() {
        return tipoDocumento == TipoDocumento.SIN_DOCUMENTO ? "Sin documento" : tipoDocumento.name() + " " + numeroDocumento;
    }

    public boolean esMenorDeEdad() {
        return edad < MAYORIA_DE_EDAD;
    }

    public boolean tieneAlergias() {
        return alergias != null && !alergias.isBlank();
    }
}
