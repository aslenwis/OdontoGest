package com.odontogest.persistencia.conversor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Converter(autoApply = true)
public class LocalDateConverterASEJ implements AttributeConverter<LocalDate, String> {

    public static final DateTimeFormatter FORMATOASEJ = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public String convertToDatabaseColumn(LocalDate valorASEJ) {
        return valorASEJ == null ? null : FORMATOASEJ.format(valorASEJ);
    }

    @Override
    public LocalDate convertToEntityAttribute(String textoASEJ) {
        return textoASEJ == null || textoASEJ.isBlank() ? null : LocalDate.parse(textoASEJ, FORMATOASEJ);
    }
}
