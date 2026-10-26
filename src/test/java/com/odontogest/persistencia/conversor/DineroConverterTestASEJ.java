package com.odontogest.persistencia.conversor;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DineroConverterTestASEJ {

    private final DineroConverterASEJ conversorASEJ = new DineroConverterASEJ();

    @Test
    void convierteSinPerderCentimosASEJ() {
        assertThat(conversorASEJ.convertToDatabaseColumn(new BigDecimal("0.10"))).isEqualTo(10L);
        assertThat(conversorASEJ.convertToDatabaseColumn(new BigDecimal("1000"))).isEqualTo(100000L);
        assertThat(conversorASEJ.convertToDatabaseColumn(new BigDecimal("333.3"))).isEqualTo(33330L);
        assertThat(conversorASEJ.convertToEntityAttribute(33334L)).isEqualTo(new BigDecimal("333.34"));
        assertThat(conversorASEJ.convertToDatabaseColumn(null)).isNull();
        assertThat(conversorASEJ.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void rechazaMasDeDosDecimalesASEJ() {
        assertThatThrownBy(() -> conversorASEJ.convertToDatabaseColumn(new BigDecimal("1.005")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
