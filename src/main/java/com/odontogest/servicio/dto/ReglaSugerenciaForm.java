/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.EstadoHallazgo;
import com.odontogest.dominio.odontograma.TipoRadicular;

/**
 * Datos del formulario de regla de sugerencia.
 *
 * @param hallazgoTipoId {@code null} = regla general (toda la boca)
 */
public record ReglaSugerenciaForm(Long id, Long hallazgoTipoId, EstadoHallazgo estado, TipoRadicular tipoRadicular,
                                  int minSuperficies, int maxSuperficies, Long procedimientoId, int fase,
                                  int prioridad, boolean activo) {
}
