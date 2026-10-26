package com.odontogest.dominio.finanzas;

import java.time.LocalDate;

public enum FrecuenciaASEJ {
    SEMANAL, QUINCENAL, MENSUAL;

    public LocalDate vencimientoASEJ(LocalDate primerVencimientoASEJ, int iASEJ) {
        return switch (this) {
            case SEMANAL -> primerVencimientoASEJ.plusWeeks(iASEJ - 1L);
            case QUINCENAL -> primerVencimientoASEJ.plusDays(15L * (iASEJ - 1));
            case MENSUAL -> primerVencimientoASEJ.plusMonths(iASEJ - 1L);
        };
    }
}
