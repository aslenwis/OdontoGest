package com.odontogest.dominio.tratamiento;

import com.odontogest.dominio.odontograma.SuperficieASEJ;

import java.util.Set;

public record ItemSugeridoASEJ(long procedimientoIdASEJ, Integer piezaFdiASEJ, Set<SuperficieASEJ> superficiesASEJ, int faseASEJ,
                           int cantidadASEJ, long precioUnitCentASEJ, Long origenHallazgoIdASEJ) {

    public ItemSugeridoASEJ {
        superficiesASEJ = Set.copyOf(superficiesASEJ);
    }

    public long totalCentASEJ() {
        return Math.multiplyExact(cantidadASEJ, precioUnitCentASEJ);
    }
}
