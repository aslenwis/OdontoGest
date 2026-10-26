package com.odontogest.servicio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MorosoDtoASEJ(long pacienteIdASEJ, String numeroHistoriaASEJ, String pacienteASEJ, String telefonoASEJ, int cuotasVencidasASEJ,
                        BigDecimal saldoVencidoASEJ, LocalDate vencimientoMasAntiguoASEJ, long diasAtrasoASEJ) {
}
