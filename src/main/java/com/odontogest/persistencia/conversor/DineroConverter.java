/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.conversor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Convierte montos {@link BigDecimal} (soles) a céntimos {@code INTEGER} en las columnas {@code *_cent}.
 * <p>
 * No es {@code autoApply}: se declara con {@code @Convert(converter = DineroConverter.class)} en cada
 * atributo de dinero. Rechaza montos con más de dos decimales en lugar de redondearlos en silencio.
 */
@Converter
public class DineroConverter implements AttributeConverter<BigDecimal, Long> {

    @Override
    public Long convertToDatabaseColumn(BigDecimal monto) {
        return monto == null ? null : aCentimos(monto);
    }

    @Override
    public BigDecimal convertToEntityAttribute(Long centimos) {
        return centimos == null ? null : desdeCentimos(centimos);
    }

    public static long aCentimos(BigDecimal monto) {
        try {
            return monto.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Monto con más de dos decimales o fuera de rango: " + monto, e);
        }
    }

    public static BigDecimal desdeCentimos(long centimos) {
        return BigDecimal.valueOf(centimos, 2);
    }
}
