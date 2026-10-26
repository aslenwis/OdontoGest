package com.odontogest.persistencia.conversor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;

@Converter
public class DiaSemanaConverterASEJ implements AttributeConverter<DayOfWeek, Integer> {

    @Override
    public Integer convertToDatabaseColumn(DayOfWeek diaASEJ) {
        return diaASEJ == null ? null : diaASEJ.getValue();
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Integer valorASEJ) {
        return valorASEJ == null ? null : DayOfWeek.of(valorASEJ);
    }
}
