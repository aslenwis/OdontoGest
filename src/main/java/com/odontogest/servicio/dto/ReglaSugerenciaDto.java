/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.TipoRadicular;

/**
 * Regla del motor de sugerencias.
 *
 * @param hallazgoTipoId {@code null} = regla general
 */
public record ReglaSugerenciaDto(long id, Long hallazgoTipoId, String hallazgoTipoNombre, EstadoHallazgo estado,
                                 TipoRadicular tipoRadicular, int minSuperficies, int maxSuperficies,
                                 long procedimientoId, String procedimientoNombre, int fase, int prioridad,
                                 boolean activo) {
}
