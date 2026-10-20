/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.conversor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Guarda {@link LocalTime} como TEXT {@code HH:mm} (los horarios no usan segundos).
 */
@Converter(autoApply = true)
public class LocalTimeConverter implements AttributeConverter<LocalTime, String> {

    public static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public String convertToDatabaseColumn(LocalTime valor) {
        return valor == null ? null : FORMATO.format(valor);
    }

    @Override
    public LocalTime convertToEntityAttribute(String texto) {
        return texto == null || texto.isBlank() ? null : LocalTime.parse(texto, DateTimeFormatter.ISO_LOCAL_TIME);
    }
}
