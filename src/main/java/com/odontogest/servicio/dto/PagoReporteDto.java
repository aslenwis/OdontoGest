/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.MedioPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pago no anulado del periodo (detalle del reporte de ingresos).
 */
public record PagoReporteDto(LocalDateTime fecha, String numeroRecibo, String numeroHistoria, String paciente,
                             MedioPago medio, String referencia, BigDecimal monto) {
}
