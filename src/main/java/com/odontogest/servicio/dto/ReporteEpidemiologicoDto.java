/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Reporte epidemiológico (RF-REP-05): pacientes cuyo último odontograma firmado se cerró en el periodo, con los
 * índices de lo firmado, agrupados por edad a la fecha de ese cierre.
 *
 * @param hasta último día incluido
 */
public record ReporteEpidemiologicoDto(LocalDate desde, LocalDate hasta, int pacientes, List<GrupoIndicesDto> grupos,
                                       boolean excluyeTercerasMolares) {

    public ReporteEpidemiologicoDto {
        grupos = List.copyOf(grupos);
    }
}
