/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Parte de un pago aplicada a una cuota: en la vista previa FIFO y en el recibo.
 *
 * @param saldoAntes saldo de la cuota antes del pago (solo en la vista previa; {@code null} en pagos registrados)
 */
public record AplicacionDto(long cuotaId, long planId, int numeroCuota, LocalDate vencimiento, BigDecimal saldoAntes,
                            BigDecimal monto) {

    /** Saldo de la cuota después del pago ({@code null} si no se conoce el saldo previo). */
    public BigDecimal saldoDespues() {
        return saldoAntes == null ? null : saldoAntes.subtract(monto);
    }

    public String etiqueta() {
        return "Plan N° " + planId + " · " + (numeroCuota == 0 ? "Inicial" : "Cuota " + numeroCuota);
    }
}
