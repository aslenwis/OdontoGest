/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.dominio.tratamiento.FormaCobro;
import com.odontogest.modelo.CategoriaProcedimiento;

import java.math.BigDecimal;

/**
 * Procedimiento del tarifario.
 */
public record ProcedimientoDto(long id, String codigo, String nombre, CategoriaProcedimiento categoria,
                               BigDecimal precio, FormaCobro formaCobro, int duracionMin, boolean activo) {

    @Override
    public String toString() {
        return codigo + " · " + nombre;
    }
}
