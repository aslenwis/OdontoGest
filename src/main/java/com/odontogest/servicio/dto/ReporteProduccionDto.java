/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Producción por odontólogo (RF-REP-03): procedimientos realizados en el periodo.
 *
 * @param hasta          último día incluido
 * @param porOdontologo  total por odontólogo, para el gráfico
 */
public record ReporteProduccionDto(LocalDate desde, LocalDate hasta, List<ProduccionFilaDto> filas,
                                   List<MontoPeriodoDto> porOdontologo, int procedimientos, BigDecimal total) {

    public ReporteProduccionDto {
        filas = List.copyOf(filas);
        porOdontologo = List.copyOf(porOdontologo);
    }
}
