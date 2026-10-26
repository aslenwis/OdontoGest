package com.odontogest.servicio.dto;

import com.odontogest.dominio.finanzas.EstadoCuotaASEJ;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuotaDtoASEJ(long idASEJ, int numeroASEJ, LocalDate vencimientoASEJ, BigDecimal montoASEJ, BigDecimal pagadoASEJ, EstadoCuotaASEJ estadoASEJ) {

    public BigDecimal saldoASEJ() {
        return montoASEJ.subtract(pagadoASEJ);
    }

    public String etiquetaASEJ() {
        return numeroASEJ == 0 ? "Inicial" : "Cuota " + numeroASEJ;
    }
}
