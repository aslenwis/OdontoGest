package com.odontogest.dominio.tratamiento;

import java.util.Objects;

public record ProcedimientoDefASEJ(long idASEJ, String codigoASEJ, String nombreASEJ, long precioCentASEJ, FormaCobroASEJ formaCobroASEJ) {

    public ProcedimientoDefASEJ {
        Objects.requireNonNull(formaCobroASEJ, "formaCobro");
        if (precioCentASEJ < 0) {
            throw new IllegalArgumentException("Precio negativo en " + codigoASEJ);
        }
    }
}
