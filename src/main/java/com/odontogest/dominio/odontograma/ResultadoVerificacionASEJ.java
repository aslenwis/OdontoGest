package com.odontogest.dominio.odontograma;

public record ResultadoVerificacionASEJ(boolean integraASEJ, Long odontogramaIdInvalidoASEJ, String motivoASEJ) {

    public static ResultadoVerificacionASEJ correctaASEJ() {
        return new ResultadoVerificacionASEJ(true, null, null);
    }

    public static ResultadoVerificacionASEJ fallaASEJ(long odontogramaIdASEJ, String motivoASEJ) {
        return new ResultadoVerificacionASEJ(false, odontogramaIdASEJ, motivoASEJ);
    }
}
