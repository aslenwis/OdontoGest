package com.odontogest.persistencia.conversor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Converter(autoApply = true)
public class LocalDateTimeConverterASEJ implements AttributeConverter<LocalDateTime, String> {

    public static final DateTimeFormatter FORMATOASEJ = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    @Override
    public String convertToDatabaseColumn(LocalDateTime valorASEJ) {
        return aTextoASEJ(valorASEJ);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String textoASEJ) {
        return textoASEJ == null || textoASEJ.isBlank() ? null : LocalDateTime.parse(textoASEJ, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static String aTextoASEJ(LocalDateTime valorASEJ) {
        return valorASEJ == null ? null : FORMATOASEJ.format(valorASEJ);
    }
}
