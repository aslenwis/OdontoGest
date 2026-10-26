package com.odontogest.dominio.odontograma;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record OdontogramaCanonicoASEJ(
        long idASEJ, long pacienteIdASEJ, String tipoASEJ, String denticionASEJ,
        LocalDateTime cerradoEnASEJ, long cerradoPorASEJ, String especificacionesASEJ,
        List<LineaASEJ> hallazgosASEJ, String hashAnteriorASEJ, String hashCierreASEJ) {

    public OdontogramaCanonicoASEJ {
        Objects.requireNonNull(cerradoEnASEJ, "cerradoEn");
        hallazgosASEJ = List.copyOf(hallazgosASEJ);
    }

    public record LineaASEJ(long idASEJ, String codigoTipoASEJ, int piezaASEJ, Integer piezaFinASEJ, int superficiesASEJ,
                        String estadoASEJ, String siglaASEJ, LocalDateTime registradoEnASEJ, long registradoPorASEJ) {

        public LineaASEJ {
            Objects.requireNonNull(registradoEnASEJ, "registradoEn");
        }
    }
}
