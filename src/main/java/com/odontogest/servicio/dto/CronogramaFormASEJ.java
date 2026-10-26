package com.odontogest.servicio.dto;

import com.odontogest.dominio.finanzas.FrecuenciaASEJ;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CronogramaFormASEJ(long planIdASEJ, BigDecimal inicialASEJ, int numeroCuotasASEJ, LocalDate primerVencimientoASEJ,
                             FrecuenciaASEJ frecuenciaASEJ) {
}
