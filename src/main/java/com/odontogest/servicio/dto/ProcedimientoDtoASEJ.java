package com.odontogest.servicio.dto;

import com.odontogest.dominio.tratamiento.FormaCobroASEJ;
import com.odontogest.modelo.CategoriaProcedimientoASEJ;

import java.math.BigDecimal;

public record ProcedimientoDtoASEJ(long idASEJ, String codigoASEJ, String nombreASEJ, CategoriaProcedimientoASEJ categoriaASEJ,
                               BigDecimal precioASEJ, FormaCobroASEJ formaCobroASEJ, int duracionMinASEJ, boolean activoASEJ) {

    @Override
    public String toString() {
        return codigoASEJ + " · " + nombreASEJ;
    }
}
