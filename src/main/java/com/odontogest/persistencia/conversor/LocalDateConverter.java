/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.conversor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Guarda {@link LocalDate} como TEXT ISO-8601 {@code yyyy-MM-dd}.
 */
@Converter(autoApply = true)
public class LocalDateConverter implements AttributeConverter<LocalDate, String> {

    public static final DateTimeFormatter FORMATO = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public String convertToDatabaseColumn(LocalDate valor) {
        return valor == null ? null : FORMATO.format(valor);
    }

    @Override
    public LocalDate convertToEntityAttribute(String texto) {
        return texto == null || texto.isBlank() ? null : LocalDate.parse(texto, FORMATO);
    }
}
