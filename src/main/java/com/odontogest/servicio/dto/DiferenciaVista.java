/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.DiferenciaOdontograma;

import java.time.LocalDateTime;

/**
 * Comparación entre dos momentos del odontograma ({@code objetivo == null} = estado actual).
 */
public record DiferenciaVista(LocalDateTime base, LocalDateTime objetivo, DiferenciaOdontograma diferencia) {
}
