/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.math.BigDecimal;

/**
 * Datos del recibo interno en PDF (RF-CAJ-02).
 *
 * @param saldoPendiente saldo del paciente al emitir el recibo
 */
public record ReciboDocumento(DatosClinica clinica, PacienteDetalle paciente, PagoDto pago, BigDecimal saldoPendiente) {
}
