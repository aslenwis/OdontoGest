/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.persistencia.conversor;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Conversión exacta soles ↔ céntimos.
 */
class DineroConverterTest {

    private final DineroConverter conversor = new DineroConverter();

    @Test
    void convierteSinPerderCentimos() {
        assertThat(conversor.convertToDatabaseColumn(new BigDecimal("0.10"))).isEqualTo(10L);
        assertThat(conversor.convertToDatabaseColumn(new BigDecimal("1000"))).isEqualTo(100000L);
        assertThat(conversor.convertToDatabaseColumn(new BigDecimal("333.3"))).isEqualTo(33330L);
        assertThat(conversor.convertToEntityAttribute(33334L)).isEqualTo(new BigDecimal("333.34"));
        assertThat(conversor.convertToDatabaseColumn(null)).isNull();
        assertThat(conversor.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void rechazaMasDeDosDecimales() {
        assertThatThrownBy(() -> conversor.convertToDatabaseColumn(new BigDecimal("1.005")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
