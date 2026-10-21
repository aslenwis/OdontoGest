/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.math.BigDecimal;

/**
 * Monto de un periodo (día, mes u odontólogo) para los gráficos de barras del tablero y los reportes.
 *
 * @param etiqueta texto del eje, p. ej. "oct. 2026"
 */
public record MontoPeriodoDto(String etiqueta, BigDecimal monto) {
}
