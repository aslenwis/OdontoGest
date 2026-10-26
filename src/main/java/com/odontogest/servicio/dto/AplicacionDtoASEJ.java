package com.odontogest.servicio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AplicacionDtoASEJ(long cuotaIdASEJ, long planIdASEJ, int numeroCuotaASEJ, LocalDate vencimientoASEJ, BigDecimal saldoAntesASEJ,
                            BigDecimal montoASEJ) {

    public BigDecimal saldoDespuesASEJ() {
        return saldoAntesASEJ == null ? null : saldoAntesASEJ.subtract(montoASEJ);
    }

    public String etiquetaASEJ() {
        return "Plan N° " + planIdASEJ + " · " + (numeroCuotaASEJ == 0 ? "Inicial" : "Cuota " + numeroCuotaASEJ);
    }
}
