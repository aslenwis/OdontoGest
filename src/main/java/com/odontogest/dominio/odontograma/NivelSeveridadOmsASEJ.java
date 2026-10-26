package com.odontogest.dominio.odontograma;

public enum NivelSeveridadOmsASEJ {
    MUY_BAJO(1.2), BAJO(2.7), MODERADO(4.5), ALTO(6.6), MUY_ALTO(Double.POSITIVE_INFINITY);

    private final double limiteSuperiorASEJ;

    NivelSeveridadOmsASEJ(double limiteSuperiorASEJ) {
        this.limiteSuperiorASEJ = limiteSuperiorASEJ;
    }

    public static NivelSeveridadOmsASEJ deASEJ(double valorASEJ) {
        if (valorASEJ < 0 || Double.isNaN(valorASEJ)) {
            throw new IllegalArgumentException("Valor de índice inválido: " + valorASEJ);
        }
        double redondeadoASEJ = Math.round(valorASEJ * 10) / 10.0;
        for (NivelSeveridadOmsASEJ nivelASEJ : values()) {
            if (redondeadoASEJ < nivelASEJ.limiteSuperiorASEJ) {
                return nivelASEJ;
            }
        }
        return MUY_ALTO;
    }
}
