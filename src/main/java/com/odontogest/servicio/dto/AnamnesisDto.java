/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Anamnesis del paciente; se usa para leer y para guardar ({@code actualizadoEn} se ignora al guardar).
 *
 * @param enfermedades clave de enfermedad sistémica → marcada (claves de {@code AnamnesisService.ENFERMEDADES})
 */
public record AnamnesisDto(
        long pacienteId, String antecedentesMedicos, String alergias, String medicacionActual,
        Map<String, Boolean> enfermedades, String habitos, boolean fuma, boolean gestante,
        String presionArterial, String observaciones, LocalDateTime actualizadoEn) {

    public AnamnesisDto {
        enfermedades = enfermedades == null ? Map.of() : Map.copyOf(enfermedades);
    }
}
