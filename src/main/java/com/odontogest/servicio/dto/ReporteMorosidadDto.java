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
 * Morosidad a una fecha (RF-REP-04): pacientes con cuotas vencidas, de mayor a menor deuda.
 */
public record ReporteMorosidadDto(LocalDate fecha, List<MorosoDto> pacientes, int cuotas, BigDecimal total) {

    public ReporteMorosidadDto {
        pacientes = List.copyOf(pacientes);
    }
}
