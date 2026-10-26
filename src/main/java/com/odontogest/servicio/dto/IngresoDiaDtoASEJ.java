package com.odontogest.servicio.dto;

import com.odontogest.modelo.MedioPagoASEJ;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record IngresoDiaDtoASEJ(LocalDate diaASEJ, int pagosASEJ, Map<MedioPagoASEJ, BigDecimal> porMedioASEJ, BigDecimal totalASEJ) {

    public IngresoDiaDtoASEJ {
        porMedioASEJ = Map.copyOf(porMedioASEJ);
    }

    public BigDecimal montoASEJ(MedioPagoASEJ medioASEJ) {
        return porMedioASEJ.getOrDefault(medioASEJ, BigDecimal.ZERO.setScale(2));
    }
}
