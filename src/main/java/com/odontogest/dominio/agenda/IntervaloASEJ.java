package com.odontogest.dominio.agenda;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public record IntervaloASEJ(LocalDateTime inicioASEJ, LocalDateTime finASEJ) {

    public IntervaloASEJ {
        Objects.requireNonNull(inicioASEJ, "inicio");
        Objects.requireNonNull(finASEJ, "fin");
        if (!finASEJ.isAfter(inicioASEJ)) {
            throw new IllegalArgumentException("El fin debe ser posterior al inicio: " + inicioASEJ + " / " + finASEJ);
        }
    }

    public static IntervaloASEJ deASEJ(LocalDateTime inicioASEJ, Duration duracionASEJ) {
        return new IntervaloASEJ(inicioASEJ, inicioASEJ.plus(duracionASEJ));
    }

    public boolean solapaASEJ(IntervaloASEJ otroASEJ) {
        return inicioASEJ.isBefore(otroASEJ.finASEJ) && otroASEJ.inicioASEJ.isBefore(finASEJ);
    }

    public boolean contieneASEJ(IntervaloASEJ otroASEJ) {
        return !otroASEJ.inicioASEJ.isBefore(inicioASEJ) && !otroASEJ.finASEJ.isAfter(finASEJ);
    }

    public Duration duracionASEJ() {
        return Duration.between(inicioASEJ, finASEJ);
    }
}
