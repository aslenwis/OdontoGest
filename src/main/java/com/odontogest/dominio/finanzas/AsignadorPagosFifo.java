/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.finanzas;

import com.odontogest.dominio.comun.ReglaDominioException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reparte un pago entre las cuotas pendientes en orden FIFO: primero la de vencimiento más antiguo
 * (a igual vencimiento, la de menor número).
 */
public final class AsignadorPagosFifo {

    public List<Aplicacion> asignar(long montoCent, List<SaldoCuota> pendientes) {
        if (montoCent <= 0) {
            throw new ReglaDominioException("El monto del pago debe ser mayor que cero.");
        }
        List<SaldoCuota> ordenadas = pendientes.stream()
                .filter(c -> c.saldoCent() > 0)
                .sorted(Comparator.comparing(SaldoCuota::vencimiento).thenComparingInt(SaldoCuota::numero))
                .toList();

        List<Aplicacion> aplicaciones = new ArrayList<>();
        long restante = montoCent;
        for (SaldoCuota cuota : ordenadas) {
            if (restante == 0) {
                break;
            }
            long aplicar = Math.min(restante, cuota.saldoCent());
            aplicaciones.add(new Aplicacion(cuota.cuotaId(), aplicar));
            restante -= aplicar;
        }
        if (restante > 0) {
            throw new ReglaDominioException("El monto excede el saldo pendiente en " + soles(restante) + ".");
        }
        return List.copyOf(aplicaciones);
    }

    private static String soles(long centimos) {
        return String.format("S/ %d.%02d", centimos / 100, centimos % 100);
    }
}
