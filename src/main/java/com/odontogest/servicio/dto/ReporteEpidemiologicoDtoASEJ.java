package com.odontogest.servicio.dto;

import java.time.LocalDate;
import java.util.List;

public record ReporteEpidemiologicoDtoASEJ(LocalDate desdeASEJ, LocalDate hastaASEJ, int pacientesASEJ, List<GrupoIndicesDtoASEJ> gruposASEJ,
                                       boolean excluyeTercerasMolaresASEJ) {

    public ReporteEpidemiologicoDtoASEJ {
        gruposASEJ = List.copyOf(gruposASEJ);
    }
}
