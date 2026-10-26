package com.odontogest.servicio.dto;

public record OpcionDtoASEJ(String claveASEJ, String etiquetaASEJ) {

    @Override
    public String toString() {
        return etiquetaASEJ;
    }
}
