/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.finanzas;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Estado de una cuota, derivado (no se guarda en la base de datos):
 * <pre>
 * PAGADA    si pagado = monto
 * VENCIDA   si vencimiento &lt; hoy y pagado &lt; monto
 * PARCIAL   si 0 &lt; pagado &lt; monto
 * PENDIENTE en otro caso
 * </pre>
 */
public enum EstadoCuota {
    PENDIENTE, PARCIAL, VENCIDA, PAGADA;

    public static EstadoCuota de(long montoCent, long pagadoCent, LocalDate vencimiento, LocalDate hoy) {
        Objects.requireNonNull(vencimiento, "vencimiento");
        Objects.requireNonNull(hoy, "hoy");
        if (pagadoCent < 0 || pagadoCent > montoCent) {
            throw new IllegalArgumentException("Pagado fuera de rango: " + pagadoCent + " de " + montoCent);
        }
        if (pagadoCent == montoCent) {
            return PAGADA;
        }
        if (vencimiento.isBefore(hoy)) {
            return VENCIDA;
        }
        return pagadoCent > 0 ? PARCIAL : PENDIENTE;
    }
}
