/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.time.LocalDateTime;

/**
 * Registro de auditoría para consultarlo en pantalla.
 *
 * @param detalle JSON con los datos de la acción ({@code null} si no hay)
 */
public record AuditoriaDto(long id, LocalDateTime fecha, String usuario, String accion, String entidad, Long entidadId,
                           String detalle) {
}
