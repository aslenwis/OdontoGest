/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.finanzas;

import com.odontogest.dominio.comun.ReglaDominioException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Cronograma de cuotas con reparto exacto de céntimos.
 * <pre>
 * R = total − inicial ;  base = R / n ;  residuo = R mod n
 * cuota_i = base + (i ≤ residuo ? 1 : 0)   ⇒  Σ cuota_i = R exactamente
 * </pre>
 * Los céntimos sobrantes van a las primeras cuotas. Un vencimiento en domingo pasa al lunes siguiente.
 */
public final class GeneradorCronograma {

    /**
     * @param totalCent         total del plan
     * @param inicialCent       cuota inicial (0..total); si es mayor que 0 se genera la cuota N° 0 con vencimiento hoy
     * @param numeroCuotas      n ≥ 1 (0 si la inicial cubre el total)
     * @param primerVencimiento vencimiento de la cuota 1 (no anterior a hoy)
     */
    public List<CuotaProgramada> generar(long totalCent, long inicialCent, int numeroCuotas,
                                         LocalDate primerVencimiento, Frecuencia frecuencia, LocalDate hoy) {
        Objects.requireNonNull(hoy, "hoy");
        if (totalCent <= 0) {
            throw new ReglaDominioException("El total del plan debe ser mayor que cero.");
        }
        if (inicialCent < 0 || inicialCent > totalCent) {
            throw new ReglaDominioException("La cuota inicial debe estar entre 0 y el total del plan.");
        }
        long restante = totalCent - inicialCent;
        if (restante == 0 && numeroCuotas != 0) {
            throw new ReglaDominioException("La cuota inicial cubre el total: no corresponden más cuotas.");
        }
        if (restante > 0) {
            if (numeroCuotas < 1) {
                throw new ReglaDominioException("Indique al menos una cuota para el saldo.");
            }
            if (numeroCuotas > restante) {
                throw new ReglaDominioException("Hay más cuotas que céntimos por repartir.");
            }
            Objects.requireNonNull(frecuencia, "frecuencia");
            Objects.requireNonNull(primerVencimiento, "primerVencimiento");
            if (primerVencimiento.isBefore(hoy)) {
                throw new ReglaDominioException("El primer vencimiento no puede ser anterior a hoy.");
            }
        }

        List<CuotaProgramada> cuotas = new ArrayList<>();
        if (inicialCent > 0) {
            cuotas.add(new CuotaProgramada(0, hoy, inicialCent));
        }
        if (restante > 0) {
            long base = restante / numeroCuotas;
            long residuo = restante % numeroCuotas;
            for (int i = 1; i <= numeroCuotas; i++) {
                long monto = base + (i <= residuo ? 1 : 0);
                cuotas.add(new CuotaProgramada(i, evitarDomingo(frecuencia.vencimiento(primerVencimiento, i)), monto));
            }
        }
        return List.copyOf(cuotas);
    }

    private static LocalDate evitarDomingo(LocalDate fecha) {
        return fecha.getDayOfWeek() == DayOfWeek.SUNDAY ? fecha.plusDays(1) : fecha;
    }
}
