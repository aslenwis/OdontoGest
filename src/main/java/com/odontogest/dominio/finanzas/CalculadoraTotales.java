/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.finanzas;

import com.odontogest.dominio.comun.ReglaDominioException;

import java.util.List;

/**
 * Totales de un plan de tratamiento, todo en céntimos.
 * <pre>
 * total_item = cantidad × precio_unit − descuento_item      (descuento_item ≤ cantidad × precio_unit)
 * subtotal   = Σ total_item de los ítems no cancelados
 * total      = subtotal − descuento_global                  (descuento_global ≤ subtotal)
 * </pre>
 */
public final class CalculadoraTotales {

    /** Línea del plan para el cálculo. */
    public record Linea(int cantidad, long precioUnitCent, long descuentoCent, boolean cancelada) {
    }

    /** Resultado: coincide con las columnas {@code subtotal_cent}, {@code descuento_cent} y {@code total_cent}. */
    public record Totales(long subtotalCent, long descuentoCent, long totalCent) {
    }

    public long totalItem(int cantidad, long precioUnitCent, long descuentoCent) {
        if (cantidad <= 0) {
            throw new ReglaDominioException("La cantidad debe ser mayor que cero.");
        }
        if (precioUnitCent < 0) {
            throw new ReglaDominioException("El precio no puede ser negativo.");
        }
        long bruto = Math.multiplyExact(cantidad, precioUnitCent);
        if (descuentoCent < 0 || descuentoCent > bruto) {
            throw new ReglaDominioException("El descuento del ítem debe estar entre 0 y su importe.");
        }
        return bruto - descuentoCent;
    }

    public Totales calcular(List<Linea> lineas, long descuentoGlobalCent) {
        long subtotal = 0;
        for (Linea l : lineas) {
            if (!l.cancelada()) {
                subtotal = Math.addExact(subtotal, totalItem(l.cantidad(), l.precioUnitCent(), l.descuentoCent()));
            }
        }
        if (descuentoGlobalCent < 0 || descuentoGlobalCent > subtotal) {
            throw new ReglaDominioException("El descuento global debe estar entre 0 y el subtotal.");
        }
        return new Totales(subtotal, descuentoGlobalCent, subtotal - descuentoGlobalCent);
    }
}
