package com.odontogest.servicio.dto;

import com.odontogest.modelo.MedioPagoASEJ;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ReporteIngresosDtoASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ, List<IngresoDiaDtoASEJ> diasASEJ,
                                 Map<MedioPagoASEJ, BigDecimal> totalesPorMedioASEJ, int pagosASEJ, BigDecimal totalASEJ,
                                 List<MontoPeriodoDtoASEJ> serieASEJ, List<PagoReporteDtoASEJ> detalleASEJ) {

    public ReporteIngresosDtoASEJ {
        diasASEJ = List.copyOf(diasASEJ);
        totalesPorMedioASEJ = Map.copyOf(totalesPorMedioASEJ);
        serieASEJ = List.copyOf(serieASEJ);
        detalleASEJ = List.copyOf(detalleASEJ);
    }
}
