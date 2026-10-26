package com.odontogest.dominio.odontograma;

import java.util.List;
import java.util.Objects;

public record TipoHallazgoDefASEJ(
        long idASEJ, String codigoASEJ, String nombreASEJ, String siglaASEJ, List<String> siglasOpcionesASEJ,
        RepresentacionASEJ representacionASEJ, SimboloHallazgoASEJ simboloASEJ, ReglaColorASEJ reglaColorASEJ, String grupoExclusionASEJ,
        boolean ocultaSuperficiesASEJ, ConteoIndiceASEJ conteoCpodASEJ, ConteoIndiceASEJ conteoCeodASEJ) {

    public TipoHallazgoDefASEJ {
        Objects.requireNonNull(codigoASEJ, "codigo");
        Objects.requireNonNull(representacionASEJ, "representacion");
        Objects.requireNonNull(simboloASEJ, "simbolo");
        Objects.requireNonNull(reglaColorASEJ, "reglaColor");
        Objects.requireNonNull(grupoExclusionASEJ, "grupoExclusion");
        siglasOpcionesASEJ = siglasOpcionesASEJ == null ? List.of() : List.copyOf(siglasOpcionesASEJ);
        conteoCpodASEJ = conteoCpodASEJ == null ? ConteoIndiceASEJ.NINGUNO : conteoCpodASEJ;
        conteoCeodASEJ = conteoCeodASEJ == null ? ConteoIndiceASEJ.NINGUNO : conteoCeodASEJ;
        if (conteoCpodASEJ == ConteoIndiceASEJ.E) {
            throw new IllegalArgumentException("El CPO-D no tiene componente E: " + codigoASEJ);
        }
        if (conteoCeodASEJ == ConteoIndiceASEJ.P) {
            throw new IllegalArgumentException("El ceo-d no tiene componente P: " + codigoASEJ);
        }
    }

    public boolean usaEstadoASEJ() {
        return reglaColorASEJ == ReglaColorASEJ.POR_ESTADO;
    }
}
