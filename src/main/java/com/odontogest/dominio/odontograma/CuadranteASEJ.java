package com.odontogest.dominio.odontograma;

public enum CuadranteASEJ {
    C1(1, ArcadaASEJ.SUPERIOR, true, false, 8),
    C2(2, ArcadaASEJ.SUPERIOR, false, false, 8),
    C3(3, ArcadaASEJ.INFERIOR, false, false, 8),
    C4(4, ArcadaASEJ.INFERIOR, true, false, 8),
    C5(5, ArcadaASEJ.SUPERIOR, true, true, 5),
    C6(6, ArcadaASEJ.SUPERIOR, false, true, 5),
    C7(7, ArcadaASEJ.INFERIOR, false, true, 5),
    C8(8, ArcadaASEJ.INFERIOR, true, true, 5);

    public final int numeroASEJ;
    public final ArcadaASEJ arcadaASEJ;
    public final boolean ladoDerechoPacienteASEJ;
    public final boolean deciduoASEJ;
    public final int piezasASEJ;

    CuadranteASEJ(int numeroASEJ, ArcadaASEJ arcadaASEJ, boolean ladoDerechoPacienteASEJ, boolean deciduoASEJ, int piezasASEJ) {
        this.numeroASEJ = numeroASEJ;
        this.arcadaASEJ = arcadaASEJ;
        this.ladoDerechoPacienteASEJ = ladoDerechoPacienteASEJ;
        this.deciduoASEJ = deciduoASEJ;
        this.piezasASEJ = piezasASEJ;
    }

    public static CuadranteASEJ deASEJ(int numeroASEJ) {
        if (numeroASEJ < 1 || numeroASEJ > 8) {
            throw new IllegalArgumentException("Cuadrante FDI inválido: " + numeroASEJ);
        }
        return values()[numeroASEJ - 1];
    }
}
