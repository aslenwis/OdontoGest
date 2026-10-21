/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.modelo;

import java.util.Set;

/**
 * Estados del plan de tratamiento (RF-PLN-04) y sus transiciones válidas:
 * <pre>
 * BORRADOR  → PROPUESTO, ANULADO
 * PROPUESTO → BORRADOR (volver a editar), ACEPTADO, ANULADO
 * ACEPTADO  → EN_CURSO, ANULADO
 * EN_CURSO  → FINALIZADO, ANULADO
 * FINALIZADO y ANULADO son finales.
 * </pre>
 * Las condiciones de cada transición (ítems, pagos, consentimiento) las valida {@code PlanTratamientoService}.
 */
public enum EstadoPlan {
    BORRADOR("Borrador"),
    PROPUESTO("Propuesto"),
    ACEPTADO("Aceptado"),
    EN_CURSO("En curso"),
    FINALIZADO("Finalizado"),
    ANULADO("Anulado");

    private final String etiqueta;

    EstadoPlan(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public Set<EstadoPlan> siguientes() {
        return switch (this) {
            case BORRADOR -> Set.of(PROPUESTO, ANULADO);
            case PROPUESTO -> Set.of(BORRADOR, ACEPTADO, ANULADO);
            case ACEPTADO -> Set.of(EN_CURSO, ANULADO);
            case EN_CURSO -> Set.of(FINALIZADO, ANULADO);
            case FINALIZADO, ANULADO -> Set.of();
        };
    }

    public boolean puedePasarA(EstadoPlan destino) {
        return siguientes().contains(destino);
    }

    /** Ítems, precios y descuentos solo se editan en borrador. */
    public boolean editable() {
        return this == BORRADOR;
    }

    /** El cronograma se genera con el total ya cerrado: propuesto, aceptado o en curso. */
    public boolean admiteCronograma() {
        return this == PROPUESTO || this == ACEPTADO || this == EN_CURSO;
    }

    /** Los procedimientos se marcan como realizados cuando el paciente ya aceptó el plan. */
    public boolean enEjecucion() {
        return this == ACEPTADO || this == EN_CURSO;
    }

    public boolean esFinal() {
        return this == FINALIZADO || this == ANULADO;
    }
}
