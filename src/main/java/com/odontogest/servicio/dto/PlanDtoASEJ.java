package com.odontogest.servicio.dto;

import com.odontogest.modelo.EstadoPlanASEJ;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record PlanDtoASEJ(long idASEJ, long pacienteIdASEJ, long odontologoIdASEJ, String odontologoNombreASEJ, String odontologoCopASEJ,
                      Long odontogramaIdASEJ, LocalDateTime fechaASEJ, EstadoPlanASEJ estadoASEJ, BigDecimal subtotalASEJ,
                      BigDecimal descuentoASEJ, BigDecimal totalASEJ, String observacionesASEJ, LocalDateTime aceptadoEnASEJ,
                      List<PlanItemDtoASEJ> itemsASEJ, List<CuotaDtoASEJ> cuotasASEJ, String consentimientoFirmadoPorASEJ,
                      LocalDateTime consentimientoEnASEJ, Set<EstadoPlanASEJ> transicionesASEJ) {

    public PlanDtoASEJ {
        itemsASEJ = List.copyOf(itemsASEJ);
        cuotasASEJ = List.copyOf(cuotasASEJ);
        transicionesASEJ = Set.copyOf(transicionesASEJ);
    }

    public long totalCentASEJ() {
        return totalASEJ.setScale(2).movePointRight(2).longValueExact();
    }

    public boolean editableASEJ() {
        return estadoASEJ.editableASEJ();
    }

    public BigDecimal pagadoASEJ() {
        return cuotasASEJ.stream().map(CuotaDtoASEJ::pagadoASEJ).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean tienePagosASEJ() {
        return pagadoASEJ().signum() > 0;
    }

    public long realizadosASEJ() {
        return itemsASEJ.stream().filter(PlanItemDtoASEJ::realizadoASEJ).count();
    }

    public String etiquetaASEJ() {
        return new PlanResumenASEJ(idASEJ, fechaASEJ, estadoASEJ, totalASEJ, odontologoNombreASEJ).etiquetaASEJ();
    }
}
