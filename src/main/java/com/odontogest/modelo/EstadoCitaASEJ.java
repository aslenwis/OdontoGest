package com.odontogest.modelo;

import java.util.Set;

public enum EstadoCitaASEJ {
    PROGRAMADA("Programada"),
    CONFIRMADA("Confirmada"),
    ATENDIDA("Atendida"),
    CANCELADA("Cancelada"),
    NO_ASISTIO("No asistió");

    private final String etiquetaASEJ;

    EstadoCitaASEJ(String etiquetaASEJ) {
        this.etiquetaASEJ = etiquetaASEJ;
    }

    public String etiquetaASEJ() {
        return etiquetaASEJ;
    }

    public boolean activaASEJ() {
        return this == PROGRAMADA || this == CONFIRMADA;
    }

    public static Set<EstadoCitaASEJ> ocupanASEJ() {
        return Set.of(PROGRAMADA, CONFIRMADA, ATENDIDA);
    }

    public boolean puedePasarAASEJ(EstadoCitaASEJ destinoASEJ) {
        return switch (this) {
            case PROGRAMADA -> destinoASEJ != PROGRAMADA;
            case CONFIRMADA -> destinoASEJ != CONFIRMADA;
            case ATENDIDA, CANCELADA, NO_ASISTIO -> false;
        };
    }
}
