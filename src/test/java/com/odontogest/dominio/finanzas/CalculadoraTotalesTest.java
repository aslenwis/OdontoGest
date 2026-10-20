/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.dominio.finanzas;

import com.odontogest.dominio.comun.ReglaDominioException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Totales del plan en céntimos.
 */
class CalculadoraTotalesTest {

    private final CalculadoraTotales calculadora = new CalculadoraTotales();

    @Test
    void totalDeItemConDescuento() {
        assertThat(calculadora.totalItem(2, 12000, 4000)).isEqualTo(20000);
        assertThat(calculadora.totalItem(1, 12000, 12000)).isZero();
    }

    @Test
    void subtotalExcluyeCanceladosYAplicaDescuentoGlobal() {
        CalculadoraTotales.Totales t = calculadora.calcular(List.of(
                new CalculadoraTotales.Linea(1, 5000, 0, false),
                new CalculadoraTotales.Linea(3, 12000, 1000, false),
                new CalculadoraTotales.Linea(1, 90000, 0, true)), 2000);
        assertThat(t).isEqualTo(new CalculadoraTotales.Totales(40000, 2000, 38000));
        assertThat(t.totalCent()).isEqualTo(t.subtotalCent() - t.descuentoCent());
    }

    @Test
    void planVacio() {
        assertThat(calculadora.calcular(List.of(), 0)).isEqualTo(new CalculadoraTotales.Totales(0, 0, 0));
    }

    @Test
    void validaciones() {
        assertThatThrownBy(() -> calculadora.totalItem(0, 100, 0)).isInstanceOf(ReglaDominioException.class);
        assertThatThrownBy(() -> calculadora.totalItem(1, -1, 0)).isInstanceOf(ReglaDominioException.class);
        assertThatThrownBy(() -> calculadora.totalItem(1, 100, 101)).isInstanceOf(ReglaDominioException.class);
        assertThatThrownBy(() -> calculadora.totalItem(1, 100, -1)).isInstanceOf(ReglaDominioException.class);
        assertThatThrownBy(() -> calculadora.calcular(List.of(new CalculadoraTotales.Linea(1, 100, 0, false)), 101))
                .isInstanceOf(ReglaDominioException.class).hasMessageContaining("descuento global");
        assertThatThrownBy(() -> calculadora.totalItem(Integer.MAX_VALUE, Long.MAX_VALUE / 2, 0))
                .isInstanceOf(ArithmeticException.class);
    }
}
