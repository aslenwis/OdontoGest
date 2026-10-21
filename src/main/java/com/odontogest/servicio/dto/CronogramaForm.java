/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.dominio.finanzas.Frecuencia;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos para generar el cronograma de cuotas (RF-PLN-05).
 *
 * @param inicial      cuota inicial ({@code null} o 0 = sin inicial)
 * @param numeroCuotas cuotas para el saldo (0 si la inicial cubre el total)
 */
public record CronogramaForm(long planId, BigDecimal inicial, int numeroCuotas, LocalDate primerVencimiento,
                             Frecuencia frecuencia) {
}
