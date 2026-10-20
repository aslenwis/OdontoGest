/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.tratamiento;

import java.util.Objects;

/**
 * Procedimiento del tarifario tal como lo necesita el motor de sugerencias.
 */
public record ProcedimientoDef(long id, String codigo, String nombre, long precioCent, FormaCobro formaCobro) {

    public ProcedimientoDef {
        Objects.requireNonNull(formaCobro, "formaCobro");
        if (precioCent < 0) {
            throw new IllegalArgumentException("Precio negativo en " + codigo);
        }
    }
}
