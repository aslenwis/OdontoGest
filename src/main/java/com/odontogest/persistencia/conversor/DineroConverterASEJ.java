package com.odontogest.persistencia.conversor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Converter
public class DineroConverterASEJ implements AttributeConverter<BigDecimal, Long> {

    @Override
    public Long convertToDatabaseColumn(BigDecimal montoASEJ) {
        return montoASEJ == null ? null : aCentimosASEJ(montoASEJ);
    }

    @Override
    public BigDecimal convertToEntityAttribute(Long centimosASEJ) {
        return centimosASEJ == null ? null : desdeCentimosASEJ(centimosASEJ);
    }

    public static long aCentimosASEJ(BigDecimal montoASEJ) {
        try {
            return montoASEJ.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
        } catch (ArithmeticException eASEJ) {
            throw new IllegalArgumentException("Monto con más de dos decimales o fuera de rango: " + montoASEJ, eASEJ);
        }
    }

    public static BigDecimal desdeCentimosASEJ(long centimosASEJ) {
        return BigDecimal.valueOf(centimosASEJ, 2);
    }
}
