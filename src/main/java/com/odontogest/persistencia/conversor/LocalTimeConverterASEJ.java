package com.odontogest.persistencia.conversor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Converter(autoApply = true)
public class LocalTimeConverterASEJ implements AttributeConverter<LocalTime, String> {

    public static final DateTimeFormatter FORMATOASEJ = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public String convertToDatabaseColumn(LocalTime valorASEJ) {
        return valorASEJ == null ? null : FORMATOASEJ.format(valorASEJ);
    }

    @Override
    public LocalTime convertToEntityAttribute(String textoASEJ) {
        return textoASEJ == null || textoASEJ.isBlank() ? null : LocalTime.parse(textoASEJ, DateTimeFormatter.ISO_LOCAL_TIME);
    }
}
