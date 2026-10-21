/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.MedioPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Reporte de ingresos por periodo y medio de pago (RF-REP-02), sin pagos anulados.
 *
 * @param hasta  último día incluido
 * @param serie  montos por día (rangos de hasta 31 días) o por mes, para el gráfico
 */
public record ReporteIngresosDto(LocalDate desde, LocalDate hasta, List<IngresoDiaDto> dias,
                                 Map<MedioPago, BigDecimal> totalesPorMedio, int pagos, BigDecimal total,
                                 List<MontoPeriodoDto> serie, List<PagoReporteDto> detalle) {

    public ReporteIngresosDto {
        dias = List.copyOf(dias);
        totalesPorMedio = Map.copyOf(totalesPorMedio);
        serie = List.copyOf(serie);
        detalle = List.copyOf(detalle);
    }
}
