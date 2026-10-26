package com.odontogest.servicio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReporteProduccionDtoASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ, List<ProduccionFilaDtoASEJ> filasASEJ,
                                   List<MontoPeriodoDtoASEJ> porOdontologoASEJ, int procedimientosASEJ, BigDecimal totalASEJ) {

    public ReporteProduccionDtoASEJ {
        filasASEJ = List.copyOf(filasASEJ);
        porOdontologoASEJ = List.copyOf(porOdontologoASEJ);
    }
}
