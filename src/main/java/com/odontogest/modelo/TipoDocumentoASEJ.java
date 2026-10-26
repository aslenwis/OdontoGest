package com.odontogest.modelo;

public enum TipoDocumentoASEJ {
    DNI("DNI"), CE("Carné de extranjería"), PASAPORTE("Pasaporte"), SIN_DOCUMENTO("Sin documento");

    public final String etiquetaASEJ;

    TipoDocumentoASEJ(String etiquetaASEJ) {
        this.etiquetaASEJ = etiquetaASEJ;
    }
}
