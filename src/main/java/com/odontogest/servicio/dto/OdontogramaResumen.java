/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.Denticion;
import com.odontogest.modelo.EstadoOdontogramaRegistro;
import com.odontogest.modelo.TipoOdontograma;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Odontograma del paciente para la línea de tiempo.
 */
public record OdontogramaResumen(long id, long atencionId, TipoOdontograma tipo, EstadoOdontogramaRegistro estado,
                                 Denticion denticion, LocalDateTime creadoEn, LocalDateTime cerradoEn, long hallazgos) {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public boolean borrador() {
        return estado == EstadoOdontogramaRegistro.BORRADOR;
    }

    /** "Inicial · 12/03/2026" o "Evolución · borrador". */
    public String etiqueta() {
        return tipo.etiqueta + " · " + (borrador() ? "borrador" : FECHA.format(cerradoEn));
    }

    @Override
    public String toString() {
        return etiqueta();
    }
}
