package com.odontogest.dominio.finanzas;

import com.odontogest.dominio.comun.ReglaDominioExceptionASEJ;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GeneradorCronogramaASEJ {

    public List<CuotaProgramadaASEJ> generarASEJ(long totalCentASEJ, long inicialCentASEJ, int numeroCuotasASEJ,
                                         LocalDate primerVencimientoASEJ, FrecuenciaASEJ frecuenciaASEJ, LocalDate hoyASEJ) {
        Objects.requireNonNull(hoyASEJ, "hoy");
        if (totalCentASEJ <= 0) {
            throw new ReglaDominioExceptionASEJ("El total del plan debe ser mayor que cero.");
        }
        if (inicialCentASEJ < 0 || inicialCentASEJ > totalCentASEJ) {
            throw new ReglaDominioExceptionASEJ("La cuota inicial debe estar entre 0 y el total del plan.");
        }
        long restanteASEJ = totalCentASEJ - inicialCentASEJ;
        if (restanteASEJ == 0 && numeroCuotasASEJ != 0) {
            throw new ReglaDominioExceptionASEJ("La cuota inicial cubre el total: no corresponden más cuotas.");
        }
        if (restanteASEJ > 0) {
            if (numeroCuotasASEJ < 1) {
                throw new ReglaDominioExceptionASEJ("Indique al menos una cuota para el saldo.");
            }
            if (numeroCuotasASEJ > restanteASEJ) {
                throw new ReglaDominioExceptionASEJ("Hay más cuotas que céntimos por repartir.");
            }
            Objects.requireNonNull(frecuenciaASEJ, "frecuencia");
            Objects.requireNonNull(primerVencimientoASEJ, "primerVencimiento");
            if (primerVencimientoASEJ.isBefore(hoyASEJ)) {
                throw new ReglaDominioExceptionASEJ("El primer vencimiento no puede ser anterior a hoy.");
            }
        }

        List<CuotaProgramadaASEJ> cuotasASEJ = new ArrayList<>();
        if (inicialCentASEJ > 0) {
            cuotasASEJ.add(new CuotaProgramadaASEJ(0, hoyASEJ, inicialCentASEJ));
        }
        if (restanteASEJ > 0) {
            long baseASEJ = restanteASEJ / numeroCuotasASEJ;
            long residuoASEJ = restanteASEJ % numeroCuotasASEJ;
            for (int iASEJ = 1; iASEJ <= numeroCuotasASEJ; iASEJ++) {
                long montoASEJ = baseASEJ + (iASEJ <= residuoASEJ ? 1 : 0);
                cuotasASEJ.add(new CuotaProgramadaASEJ(iASEJ, evitarDomingoASEJ(frecuenciaASEJ.vencimientoASEJ(primerVencimientoASEJ, iASEJ)), montoASEJ));
            }
        }
        return List.copyOf(cuotasASEJ);
    }

    private static LocalDate evitarDomingoASEJ(LocalDate fechaASEJ) {
        return fechaASEJ.getDayOfWeek() == DayOfWeek.SUNDAY ? fechaASEJ.plusDays(1) : fechaASEJ;
    }
}
