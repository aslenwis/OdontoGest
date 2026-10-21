/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.Denticion;

/**
 * Datos para abrir o actualizar una atención.
 *
 * @param id           {@code null} para abrir una atención nueva
 * @param odontologoId si el usuario es odontólogo se usa el suyo; un administrador lo elige
 * @param denticion    dentición del odontograma que se abre con la atención (solo al abrir)
 */
public record AtencionForm(Long id, long pacienteId, Long odontologoId, String motivoConsulta, String examenClinico,
                           String diagnostico, String observaciones, Denticion denticion) {
}
