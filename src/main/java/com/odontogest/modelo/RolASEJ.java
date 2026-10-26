package com.odontogest.modelo;

public enum RolASEJ {
    ADMIN("Administrador"), ODONTOLOGO("Odontólogo"), RECEPCION("Recepción");

    public final String etiquetaASEJ;

    RolASEJ(String etiquetaASEJ) {
        this.etiquetaASEJ = etiquetaASEJ;
    }
}
