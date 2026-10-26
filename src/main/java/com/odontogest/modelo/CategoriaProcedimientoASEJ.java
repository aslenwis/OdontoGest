package com.odontogest.modelo;

public enum CategoriaProcedimientoASEJ {
    DIAGNOSTICO("Diagnóstico"), PREVENTIVO("Preventivo"), OPERATORIA("Operatoria"), ENDODONCIA("Endodoncia"),
    PERIODONCIA("Periodoncia"), CIRUGIA("Cirugía"), PROTESIS("Prótesis"), IMPLANTOLOGIA("Implantología"),
    ORTODONCIA("Ortodoncia"), ESTETICA("Estética");

    public final String etiquetaASEJ;

    CategoriaProcedimientoASEJ(String etiquetaASEJ) {
        this.etiquetaASEJ = etiquetaASEJ;
    }
}
