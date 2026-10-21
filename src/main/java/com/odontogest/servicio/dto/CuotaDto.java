/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.dominio.finanzas.EstadoCuota;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cuota del cronograma con su estado derivado.
 *
 * @param numero 0 = cuota inicial
 */
public record CuotaDto(long id, int numero, LocalDate vencimiento, BigDecimal monto, BigDecimal pagado, EstadoCuota estado) {

    public BigDecimal saldo() {
        return monto.subtract(pagado);
    }

    public String etiqueta() {
        return numero == 0 ? "Inicial" : "Cuota " + numero;
    }
}
