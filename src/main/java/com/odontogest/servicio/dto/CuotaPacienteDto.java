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
 * Cuota de cualquiera de los planes cobrables del paciente (estado de cuenta y caja).
 *
 * @param numero 0 = cuota inicial
 */
public record CuotaPacienteDto(long cuotaId, long planId, int numero, LocalDate vencimiento, BigDecimal monto,
                               BigDecimal pagado, EstadoCuota estado) {

    public BigDecimal saldo() {
        return monto.subtract(pagado);
    }

    /** Por ejemplo "Plan N° 3 · Cuota 2" o "Plan N° 3 · Inicial". */
    public String etiqueta() {
        return "Plan N° " + planId + " · " + (numero == 0 ? "Inicial" : "Cuota " + numero);
    }
}
