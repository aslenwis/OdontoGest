package com.odontogest.dominio.finanzas;

import java.time.LocalDate;

public record SaldoCuotaASEJ(long cuotaIdASEJ, LocalDate vencimientoASEJ, int numeroASEJ, long saldoCentASEJ) {
}
