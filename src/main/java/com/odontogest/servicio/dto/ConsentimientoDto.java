/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.TipoConsentimiento;

import java.time.LocalDateTime;

/**
 * Consentimiento firmado.
 */
public record ConsentimientoDto(long id, TipoConsentimiento tipo, LocalDateTime aceptadoEn, String firmadoPor) {
}
