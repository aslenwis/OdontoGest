/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.conversor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Guarda {@link LocalDateTime} como TEXT ISO-8601 {@code yyyy-MM-dd'T'HH:mm:ss}
 * (sin fracciones de segundo), de modo que el texto se ordena y compara cronológicamente.
 */
@Converter(autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

    public static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    @Override
    public String convertToDatabaseColumn(LocalDateTime valor) {
        return aTexto(valor);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String texto) {
        // ISO_LOCAL_DATE_TIME acepta el formato propio y también textos con fracción de segundo.
        return texto == null || texto.isBlank() ? null : LocalDateTime.parse(texto, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /** Formato persistido; útil para SQL nativo (seeders, DAO). */
    public static String aTexto(LocalDateTime valor) {
        return valor == null ? null : FORMATO.format(valor);
    }
}
