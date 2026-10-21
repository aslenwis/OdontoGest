/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.Superficie;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Alta o edición de un ítem del plan (RF-PLN-02).
 *
 * @param id         {@code null} = ítem nuevo
 * @param piezaFdi   {@code null} = procedimiento sin pieza (toda la boca, sesión…)
 * @param cantidad   se ignora en procedimientos por superficie: la cantidad es el número de superficies
 * @param precioUnit {@code null} = precio del tarifario
 * @param descuento  {@code null} = sin descuento
 */
public record PlanItemForm(Long id, long planId, Long procedimientoId, Integer piezaFdi, Set<Superficie> superficies,
                           int fase, int cantidad, BigDecimal precioUnit, BigDecimal descuento) {

    public PlanItemForm {
        superficies = superficies == null ? Set.of() : Set.copyOf(superficies);
    }
}
