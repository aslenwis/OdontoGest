package com.odontogest.dominio.odontograma;

public enum DenticionASEJ {
    PERMANENTE, DECIDUA, MIXTA;

    public static DenticionASEJ sugeridaASEJ(int edadASEJ) {
        if (edadASEJ < 6) {
            return DECIDUA;
        }
        return edadASEJ <= 12 ? MIXTA : PERMANENTE;
    }

    public boolean admiteASEJ(PiezaDentalASEJ piezaASEJ) {
        return switch (this) {
            case PERMANENTE -> !piezaASEJ.esDeciduaASEJ();
            case DECIDUA -> piezaASEJ.esDeciduaASEJ();
            case MIXTA -> true;
        };
    }
}
