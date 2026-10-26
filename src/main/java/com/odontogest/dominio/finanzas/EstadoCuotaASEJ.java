package com.odontogest.dominio.finanzas;

import java.time.LocalDate;
import java.util.Objects;

public enum EstadoCuotaASEJ {
    PENDIENTE, PARCIAL, VENCIDA, PAGADA;

    public static EstadoCuotaASEJ deASEJ(long montoCentASEJ, long pagadoCentASEJ, LocalDate vencimientoASEJ, LocalDate hoyASEJ) {
        Objects.requireNonNull(vencimientoASEJ, "vencimiento");
        Objects.requireNonNull(hoyASEJ, "hoy");
        if (pagadoCentASEJ < 0 || pagadoCentASEJ > montoCentASEJ) {
            throw new IllegalArgumentException("Pagado fuera de rango: " + pagadoCentASEJ + " de " + montoCentASEJ);
        }
        if (pagadoCentASEJ == montoCentASEJ) {
            return PAGADA;
        }
        if (vencimientoASEJ.isBefore(hoyASEJ)) {
            return VENCIDA;
        }
        return pagadoCentASEJ > 0 ? PARCIAL : PENDIENTE;
    }
}
