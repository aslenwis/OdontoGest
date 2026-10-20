/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.tratamiento;

import com.odontogest.dominio.odontograma.Superficie;

import java.util.Set;

/**
 * Ítem de plan de tratamiento propuesto por {@link MotorSugerencias}.
 *
 * @param piezaFdi         {@code null} para procedimientos de toda la boca
 * @param cantidad         número de superficies si el procedimiento se cobra por superficie; 1 en otro caso
 * @param origenHallazgoId hallazgo que originó la sugerencia ({@code null} en reglas generales)
 */
public record ItemSugerido(long procedimientoId, Integer piezaFdi, Set<Superficie> superficies, int fase,
                           int cantidad, long precioUnitCent, Long origenHallazgoId) {

    public ItemSugerido {
        superficies = Set.copyOf(superficies);
    }

    public long totalCent() {
        return Math.multiplyExact(cantidad, precioUnitCent);
    }
}
