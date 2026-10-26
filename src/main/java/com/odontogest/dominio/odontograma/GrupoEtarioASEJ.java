package com.odontogest.dominio.odontograma;

public enum GrupoEtarioASEJ {
    INFANCIA("0 a 5 años", 0, 5, false, true),
    ESCOLAR("6 a 11 años", 6, 11, true, true),
    ADOLESCENCIA("12 a 17 años", 12, 17, true, false),
    JOVEN("18 a 29 años", 18, 29, true, false),
    ADULTO("30 a 44 años", 30, 44, true, false),
    ADULTO_MAYOR_JOVEN("45 a 59 años", 45, 59, true, false),
    ADULTO_MAYOR("60 años a más", 60, Integer.MAX_VALUE, true, false);

    public final String etiquetaASEJ;
    public final int edadMinimaASEJ;
    public final int edadMaximaASEJ;
    public final boolean aplicaCpodASEJ;
    public final boolean aplicaCeodASEJ;

    GrupoEtarioASEJ(String etiquetaASEJ, int edadMinimaASEJ, int edadMaximaASEJ, boolean aplicaCpodASEJ, boolean aplicaCeodASEJ) {
        this.etiquetaASEJ = etiquetaASEJ;
        this.edadMinimaASEJ = edadMinimaASEJ;
        this.edadMaximaASEJ = edadMaximaASEJ;
        this.aplicaCpodASEJ = aplicaCpodASEJ;
        this.aplicaCeodASEJ = aplicaCeodASEJ;
    }

    public static GrupoEtarioASEJ deASEJ(int edadASEJ) {
        if (edadASEJ < 0) {
            throw new IllegalArgumentException("Edad negativa: " + edadASEJ);
        }
        for (GrupoEtarioASEJ gASEJ : values()) {
            if (edadASEJ <= gASEJ.edadMaximaASEJ) {
                return gASEJ;
            }
        }
        return ADULTO_MAYOR;
    }
}
