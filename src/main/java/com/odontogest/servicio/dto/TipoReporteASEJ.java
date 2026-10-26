package com.odontogest.servicio.dto;

public enum TipoReporteASEJ {
    INGRESOS("Ingresos"),
    PRODUCCION("Producción por odontólogo"),
    MOROSIDAD("Morosidad"),
    EPIDEMIOLOGICO("Epidemiológico"),
    AUDITORIA("Auditoría");

    public final String etiquetaASEJ;

    TipoReporteASEJ(String etiquetaASEJ) {
        this.etiquetaASEJ = etiquetaASEJ;
    }
}
