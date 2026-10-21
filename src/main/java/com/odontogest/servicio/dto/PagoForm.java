/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.MedioPago;

import java.math.BigDecimal;

/**
 * Cobro en caja (RF-CAJ-01): el monto se reparte entre las cuotas pendientes del paciente en orden FIFO.
 *
 * @param referencia número de operación o voucher (opcional)
 */
public record PagoForm(long pacienteId, BigDecimal monto, MedioPago medio, String referencia) {
}
