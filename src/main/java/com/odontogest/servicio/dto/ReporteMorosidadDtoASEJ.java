package com.odontogest.servicio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReporteMorosidadDtoASEJ(LocalDate fechaASEJ, List<MorosoDtoASEJ> pacientesASEJ, int cuotasASEJ, BigDecimal totalASEJ) {

    public ReporteMorosidadDtoASEJ {
        pacientesASEJ = List.copyOf(pacientesASEJ);
    }
}
