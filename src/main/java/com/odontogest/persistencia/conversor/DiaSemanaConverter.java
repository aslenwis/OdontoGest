/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.conversor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;

/**
 * Guarda {@link DayOfWeek} como {@code INTEGER} ISO: 1 = lunes … 7 = domingo.
 */
@Converter
public class DiaSemanaConverter implements AttributeConverter<DayOfWeek, Integer> {

    @Override
    public Integer convertToDatabaseColumn(DayOfWeek dia) {
        return dia == null ? null : dia.getValue();
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Integer valor) {
        return valor == null ? null : DayOfWeek.of(valor);
    }
}
