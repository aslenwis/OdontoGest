package com.odontogest.modelo;

import java.util.Set;

public enum EstadoPlanASEJ {
    BORRADOR("Borrador"),
    PROPUESTO("Propuesto"),
    ACEPTADO("Aceptado"),
    EN_CURSO("En curso"),
    FINALIZADO("Finalizado"),
    ANULADO("Anulado");

    private final String etiquetaASEJ;

    EstadoPlanASEJ(String etiquetaASEJ) {
        this.etiquetaASEJ = etiquetaASEJ;
    }

    public String etiquetaASEJ() {
        return etiquetaASEJ;
    }

    public Set<EstadoPlanASEJ> siguientesASEJ() {
        return switch (this) {
            case BORRADOR -> Set.of(PROPUESTO, ANULADO);
            case PROPUESTO -> Set.of(BORRADOR, ACEPTADO, ANULADO);
            case ACEPTADO -> Set.of(EN_CURSO, ANULADO);
            case EN_CURSO -> Set.of(FINALIZADO, ANULADO);
            case FINALIZADO, ANULADO -> Set.of();
        };
    }

    public boolean puedePasarAASEJ(EstadoPlanASEJ destinoASEJ) {
        return siguientesASEJ().contains(destinoASEJ);
    }

    public boolean editableASEJ() {
        return this == BORRADOR;
    }

    public boolean admiteCronogramaASEJ() {
        return this == PROPUESTO || this == ACEPTADO || this == EN_CURSO;
    }

    public boolean enEjecucionASEJ() {
        return this == ACEPTADO || this == EN_CURSO;
    }

    public boolean esFinalASEJ() {
        return this == FINALIZADO || this == ANULADO;
    }
}
