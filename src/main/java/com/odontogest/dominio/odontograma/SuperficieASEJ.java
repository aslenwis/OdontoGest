package com.odontogest.dominio.odontograma;

public enum SuperficieASEJ {
    O(1), M(2), D(4), V(8), L(16);

    public final int bitASEJ;

    SuperficieASEJ(int bitASEJ) {
        this.bitASEJ = bitASEJ;
    }

    public String etiquetaASEJ(PiezaDentalASEJ piezaASEJ) {
        return switch (this) {
            case O -> piezaASEJ.esAnteriorASEJ() ? "I" : "O";
            case L -> piezaASEJ.arcadaASEJ() == ArcadaASEJ.SUPERIOR ? "P" : "L";
            default -> name();
        };
    }

    public String nombreASEJ(PiezaDentalASEJ piezaASEJ) {
        return switch (this) {
            case O -> piezaASEJ.esAnteriorASEJ() ? "Incisal" : "Oclusal";
            case M -> "Mesial";
            case D -> "Distal";
            case V -> "Vestibular";
            case L -> piezaASEJ.arcadaASEJ() == ArcadaASEJ.SUPERIOR ? "Palatino" : "Lingual";
        };
    }
}
