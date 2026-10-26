package com.odontogest.servicio.dto;

import com.odontogest.modelo.MedioPagoASEJ;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ResumenCajaDtoASEJ(LocalDate fechaASEJ, List<PagoDtoASEJ> pagosASEJ, Map<MedioPagoASEJ, BigDecimal> porMedioASEJ, BigDecimal totalASEJ) {

    public ResumenCajaDtoASEJ {
        pagosASEJ = List.copyOf(pagosASEJ);
        porMedioASEJ = Map.copyOf(porMedioASEJ);
    }
}
