package com.odontogest.dominio.tratamiento;

import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.HallazgoEventoASEJ;
import com.odontogest.dominio.odontograma.TipoRadicularASEJ;

public record ReglaSugerenciaDefASEJ(
        long idASEJ, Long hallazgoTipoIdASEJ, EstadoHallazgoASEJ estadoASEJ, TipoRadicularASEJ tipoRadicularASEJ,
        int minSuperficiesASEJ, int maxSuperficiesASEJ, long procedimientoIdASEJ, int faseASEJ, int prioridadASEJ) {

    public ReglaSugerenciaDefASEJ {
        if (minSuperficiesASEJ > maxSuperficiesASEJ) {
            throw new IllegalArgumentException("Rango de superficies inválido en la regla " + idASEJ);
        }
        if (faseASEJ < 1 || faseASEJ > 4) {
            throw new IllegalArgumentException("Fase fuera de rango (1–4) en la regla " + idASEJ);
        }
    }

    public boolean esGeneralASEJ() {
        return hallazgoTipoIdASEJ == null;
    }

    public boolean aplicaAASEJ(HallazgoEventoASEJ hASEJ) {
        int superficiesASEJ = hASEJ.superficiesASEJ().size();
        return hallazgoTipoIdASEJ != null && hallazgoTipoIdASEJ == hASEJ.tipoASEJ().idASEJ()
                && (estadoASEJ == null || estadoASEJ == hASEJ.estadoASEJ())
                && (tipoRadicularASEJ == null || tipoRadicularASEJ == hASEJ.piezaASEJ().tipoRadicularASEJ())
                && minSuperficiesASEJ <= superficiesASEJ && superficiesASEJ <= maxSuperficiesASEJ;
    }
}
