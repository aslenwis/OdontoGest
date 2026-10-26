package com.odontogest.modelo;

public enum MedioPagoASEJ {
    EFECTIVO("Efectivo", false),
    TARJETA("Tarjeta", true),
    YAPE("Yape", true),
    PLIN("Plin", true),
    TRANSFERENCIA("Transferencia", true);

    private final String etiquetaASEJ;
    private final boolean pideReferenciaASEJ;

    MedioPagoASEJ(String etiquetaASEJ, boolean pideReferenciaASEJ) {
        this.etiquetaASEJ = etiquetaASEJ;
        this.pideReferenciaASEJ = pideReferenciaASEJ;
    }

    public String etiquetaASEJ() {
        return etiquetaASEJ;
    }

    public boolean pideReferenciaASEJ() {
        return pideReferenciaASEJ;
    }
}
