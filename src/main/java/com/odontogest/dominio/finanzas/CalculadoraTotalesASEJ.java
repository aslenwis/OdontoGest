package com.odontogest.dominio.finanzas;

import com.odontogest.dominio.comun.ReglaDominioExceptionASEJ;

import java.util.List;

public final class CalculadoraTotalesASEJ {

    public record LineaASEJ(int cantidadASEJ, long precioUnitCentASEJ, long descuentoCentASEJ, boolean canceladaASEJ) {
    }

    public record TotalesASEJ(long subtotalCentASEJ, long descuentoCentASEJ, long totalCentASEJ) {
    }

    public long totalItemASEJ(int cantidadASEJ, long precioUnitCentASEJ, long descuentoCentASEJ) {
        if (cantidadASEJ <= 0) {
            throw new ReglaDominioExceptionASEJ("La cantidad debe ser mayor que cero.");
        }
        if (precioUnitCentASEJ < 0) {
            throw new ReglaDominioExceptionASEJ("El precio no puede ser negativo.");
        }
        long brutoASEJ = Math.multiplyExact(cantidadASEJ, precioUnitCentASEJ);
        if (descuentoCentASEJ < 0 || descuentoCentASEJ > brutoASEJ) {
            throw new ReglaDominioExceptionASEJ("El descuento del ítem debe estar entre 0 y su importe.");
        }
        return brutoASEJ - descuentoCentASEJ;
    }

    public TotalesASEJ calcularASEJ(List<LineaASEJ> lineasASEJ, long descuentoGlobalCentASEJ) {
        long subtotalASEJ = 0;
        for (LineaASEJ lASEJ : lineasASEJ) {
            if (!lASEJ.canceladaASEJ()) {
                subtotalASEJ = Math.addExact(subtotalASEJ, totalItemASEJ(lASEJ.cantidadASEJ(), lASEJ.precioUnitCentASEJ(), lASEJ.descuentoCentASEJ()));
            }
        }
        if (descuentoGlobalCentASEJ < 0 || descuentoGlobalCentASEJ > subtotalASEJ) {
            throw new ReglaDominioExceptionASEJ("El descuento global debe estar entre 0 y el subtotal.");
        }
        return new TotalesASEJ(subtotalASEJ, descuentoGlobalCentASEJ, subtotalASEJ - descuentoGlobalCentASEJ);
    }
}
