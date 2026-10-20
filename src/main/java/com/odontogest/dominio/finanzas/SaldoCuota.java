/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.finanzas;

import java.time.LocalDate;

/**
 * Cuota con saldo pendiente, entrada de {@link AsignadorPagosFifo}.
 */
public record SaldoCuota(long cuotaId, LocalDate vencimiento, int numero, long saldoCent) {
}
