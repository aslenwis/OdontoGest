package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.DenticionASEJ;
import com.odontogest.modelo.EstadoOdontogramaRegistroASEJ;
import com.odontogest.modelo.TipoOdontogramaASEJ;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record OdontogramaResumenASEJ(long idASEJ, long atencionIdASEJ, TipoOdontogramaASEJ tipoASEJ, EstadoOdontogramaRegistroASEJ estadoASEJ,
                                 DenticionASEJ denticionASEJ, LocalDateTime creadoEnASEJ, LocalDateTime cerradoEnASEJ, long hallazgosASEJ) {

    private static final DateTimeFormatter FECHAASEJ = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public boolean borradorASEJ() {
        return estadoASEJ == EstadoOdontogramaRegistroASEJ.BORRADOR;
    }

    public String etiquetaASEJ() {
        return tipoASEJ.etiquetaASEJ + " · " + (borradorASEJ() ? "borrador" : FECHAASEJ.format(cerradoEnASEJ));
    }

    @Override
    public String toString() {
        return etiquetaASEJ();
    }
}
