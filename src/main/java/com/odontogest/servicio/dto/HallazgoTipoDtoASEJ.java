package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.ConteoIndiceASEJ;
import com.odontogest.dominio.odontograma.ReglaColorASEJ;
import com.odontogest.dominio.odontograma.RepresentacionASEJ;
import com.odontogest.dominio.odontograma.SimboloHallazgoASEJ;

import java.util.List;

public record HallazgoTipoDtoASEJ(long idASEJ, String codigoASEJ, String nombreASEJ, String siglaASEJ, List<String> siglasOpcionesASEJ,
                              RepresentacionASEJ representacionASEJ, SimboloHallazgoASEJ simboloASEJ, ReglaColorASEJ reglaColorASEJ,
                              String grupoExclusionASEJ, boolean ocultaSuperficiesASEJ, ConteoIndiceASEJ conteoCpodASEJ,
                              ConteoIndiceASEJ conteoCeodASEJ, boolean activoASEJ, int ordenASEJ, boolean enUsoASEJ) {

    public HallazgoTipoDtoASEJ {
        siglasOpcionesASEJ = List.copyOf(siglasOpcionesASEJ);
    }

    @Override
    public String toString() {
        return nombreASEJ;
    }
}
