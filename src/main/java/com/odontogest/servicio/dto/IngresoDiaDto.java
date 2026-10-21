/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.MedioPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Ingresos de un día por medio de pago (fila del reporte de ingresos).
 */
public record IngresoDiaDto(LocalDate dia, int pagos, Map<MedioPago, BigDecimal> porMedio, BigDecimal total) {

    public IngresoDiaDto {
        porMedio = Map.copyOf(porMedio);
    }

    public BigDecimal monto(MedioPago medio) {
        return porMedio.getOrDefault(medio, BigDecimal.ZERO.setScale(2));
    }
}
