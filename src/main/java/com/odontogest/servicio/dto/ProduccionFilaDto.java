/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.math.BigDecimal;

/**
 * Procedimiento realizado por un odontólogo en el periodo: cantidad y monto (RF-REP-03).
 */
public record ProduccionFilaDto(long odontologoId, String odontologo, String codigo, String procedimiento,
                                int cantidad, BigDecimal monto) {
}
