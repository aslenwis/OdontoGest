/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import com.odontogest.modelo.MedioPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pago con sus aplicaciones a cuotas. En {@code aplicaciones}, {@code saldoAntes} es el saldo de la cuota justo
 * antes de este pago.
 */
public record PagoDto(long id, String numeroRecibo, long pacienteId, String pacienteNombre, String numeroHistoria,
                      LocalDateTime fecha, BigDecimal monto, MedioPago medio, String referencia, String registradoPor,
                      boolean anulado, String motivoAnulacion, LocalDateTime anuladoEn, List<AplicacionDto> aplicaciones) {

    public PagoDto {
        aplicaciones = List.copyOf(aplicaciones);
    }
}
