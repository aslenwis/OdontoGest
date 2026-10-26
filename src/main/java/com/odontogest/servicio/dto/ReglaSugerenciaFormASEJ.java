package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.EstadoHallazgoASEJ;
import com.odontogest.dominio.odontograma.TipoRadicularASEJ;

public record ReglaSugerenciaFormASEJ(Long idASEJ, Long hallazgoTipoIdASEJ, EstadoHallazgoASEJ estadoASEJ, TipoRadicularASEJ tipoRadicularASEJ,
                                  int minSuperficiesASEJ, int maxSuperficiesASEJ, Long procedimientoIdASEJ, int faseASEJ,
                                  int prioridadASEJ, boolean activoASEJ) {
}
