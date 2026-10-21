/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.EstadoAtencion;

import java.time.LocalDateTime;

/**
 * Datos clínicos de una atención.
 */
public record AtencionDetalle(long id, long pacienteId, long odontologoId, String odontologoNombre, LocalDateTime fecha,
                              String motivoConsulta, String examenClinico, String diagnostico, String observaciones,
                              EstadoAtencion estado, LocalDateTime cerradaEn) {

    public boolean abierta() {
        return estado == EstadoAtencion.ABIERTA;
    }
}
