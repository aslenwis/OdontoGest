/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.MedioPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Caja del día (RF-CAJ-05): pagos (incluidos los anulados) y totales por medio de pago sin los anulados.
 *
 * @param porMedio todos los medios, en el orden del enum (0.00 si no hubo cobros)
 */
public record ResumenCajaDto(LocalDate fecha, List<PagoDto> pagos, Map<MedioPago, BigDecimal> porMedio, BigDecimal total) {

    public ResumenCajaDto {
        pagos = List.copyOf(pagos);
        porMedio = Map.copyOf(porMedio);
    }
}
