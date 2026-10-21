/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.TipoConfiguracion;

/**
 * Clave editable de la configuración para la pantalla Configuración.
 *
 * @param valor               {@code null} en los secretos (nunca se envían a la interfaz)
 * @param secretoConfigurado  el secreto tiene un valor guardado
 */
public record ConfiguracionDto(String clave, String valor, TipoConfiguracion tipo, String grupo, String descripcion,
                               boolean secretoConfigurado) {
}
