/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Paciente con cuotas vencidas (RF-REP-04 y tablero de inicio).
 *
 * @param diasAtraso días desde el vencimiento más antiguo
 */
public record MorosoDto(long pacienteId, String numeroHistoria, String paciente, String telefono, int cuotasVencidas,
                        BigDecimal saldoVencido, LocalDate vencimientoMasAntiguo, long diasAtraso) {
}
