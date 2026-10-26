package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.TipoRadicularASEJ;

public record ReglaSugerenciaDtoASEJ(long idASEJ, Long hallazgoTipoIdASEJ, String hallazgoTipoNombreASEJ, EstadoHallazgoASEJ estadoASEJ,
                                 TipoRadicularASEJ tipoRadicularASEJ, int minSuperficiesASEJ, int maxSuperficiesASEJ,
                                 long procedimientoIdASEJ, String procedimientoNombreASEJ, int faseASEJ, int prioridadASEJ,
                                 boolean activoASEJ) {
}
