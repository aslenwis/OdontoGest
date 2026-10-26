package com.odontogest.dominio.odontograma;

public record ClaveCapaASEJ(int piezaInicioASEJ, int piezaFinASEJ, SuperficieASEJ superficieASEJ, String capaASEJ) {

    public boolean esDeRangoASEJ() {
        return piezaInicioASEJ != piezaFinASEJ;
    }
}
