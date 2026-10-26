package com.odontogest.modelo;

public enum EstadoPlanItemASEJ {
    PENDIENTE("Pendiente"),
    EN_PROCESO("En proceso"),
    REALIZADO("Realizado"),
    CANCELADO("Cancelado");

    private final String etiquetaASEJ;

    EstadoPlanItemASEJ(String etiquetaASEJ) {
        this.etiquetaASEJ = etiquetaASEJ;
    }

    public String etiquetaASEJ() {
        return etiquetaASEJ;
    }
}
