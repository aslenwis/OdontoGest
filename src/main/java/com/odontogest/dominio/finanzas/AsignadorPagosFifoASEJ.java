package com.odontogest.dominio.finanzas;

import com.odontogest.dominio.comun.ReglaDominioExceptionASEJ;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AsignadorPagosFifoASEJ {

    public List<AplicacionASEJ> asignarASEJ(long montoCentASEJ, List<SaldoCuotaASEJ> pendientesASEJ) {
        if (montoCentASEJ <= 0) {
            throw new ReglaDominioExceptionASEJ("El monto del pago debe ser mayor que cero.");
        }
        List<SaldoCuotaASEJ> ordenadasASEJ = pendientesASEJ.stream()
                .filter(cASEJ -> cASEJ.saldoCentASEJ() > 0)
                .sorted(Comparator.comparing(SaldoCuotaASEJ::vencimientoASEJ).thenComparingInt(SaldoCuotaASEJ::numeroASEJ))
                .toList();

        List<AplicacionASEJ> aplicacionesASEJ = new ArrayList<>();
        long restanteASEJ = montoCentASEJ;
        for (SaldoCuotaASEJ cuotaASEJ : ordenadasASEJ) {
            if (restanteASEJ == 0) {
                break;
            }
            long aplicarASEJ = Math.min(restanteASEJ, cuotaASEJ.saldoCentASEJ());
            aplicacionesASEJ.add(new AplicacionASEJ(cuotaASEJ.cuotaIdASEJ(), aplicarASEJ));
            restanteASEJ -= aplicarASEJ;
        }
        if (restanteASEJ > 0) {
            throw new ReglaDominioExceptionASEJ("El monto excede el saldo pendiente en " + solesASEJ(restanteASEJ) + ".");
        }
        return List.copyOf(aplicacionesASEJ);
    }

    private static String solesASEJ(long centimosASEJ) {
        return String.format("S/ %d.%02d", centimosASEJ / 100, centimosASEJ % 100);
    }
}
