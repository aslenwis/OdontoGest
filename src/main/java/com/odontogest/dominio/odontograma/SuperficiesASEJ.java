package com.odontogest.dominio.odontograma;

import java.util.Collection;
import java.util.EnumSet;

public final class SuperficiesASEJ {

    public static final int MASCARA_MAXIMAASEJ = 31;

    private SuperficiesASEJ() {
    }

    public static int aMascaraASEJ(Collection<SuperficieASEJ> superficiesASEJ) {
        int mascaraASEJ = 0;
        if (superficiesASEJ != null) {
            for (SuperficieASEJ sASEJ : superficiesASEJ) {
                mascaraASEJ |= sASEJ.bitASEJ;
            }
        }
        return mascaraASEJ;
    }

    public static EnumSet<SuperficieASEJ> desdeMascaraASEJ(int mascaraASEJ) {
        if (mascaraASEJ < 0 || mascaraASEJ > MASCARA_MAXIMAASEJ) {
            throw new IllegalArgumentException("Máscara de superficies fuera de rango: " + mascaraASEJ);
        }
        EnumSet<SuperficieASEJ> resultadoASEJ = EnumSet.noneOf(SuperficieASEJ.class);
        for (SuperficieASEJ sASEJ : SuperficieASEJ.values()) {
            if ((mascaraASEJ & sASEJ.bitASEJ) != 0) {
                resultadoASEJ.add(sASEJ);
            }
        }
        return resultadoASEJ;
    }
}
