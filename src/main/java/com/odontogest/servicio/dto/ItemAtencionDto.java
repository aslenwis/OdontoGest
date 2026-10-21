/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

/**
 * Procedimiento de un plan aceptado que se puede marcar como realizado en una atención (RF-ATE-02).
 *
 * @param realizadoAqui {@code true} si se marcó como realizado en esta atención
 * @param editable      se puede marcar o desmarcar (la atención está abierta y el plan en ejecución)
 */
public record ItemAtencionDto(long itemId, long planId, String descripcion, String fase, boolean realizadoAqui,
                              boolean editable) {
}
