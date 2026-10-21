/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Estado de cuenta del paciente (RF-CAJ-04): cuotas de sus planes aceptados, en curso o finalizados, y pagos.
 *
 * @param total   suma de las cuotas
 * @param vencido saldo de las cuotas vencidas
 */
public record EstadoCuentaDto(long pacienteId, BigDecimal total, BigDecimal pagado, BigDecimal saldo, BigDecimal vencido,
                              List<CuotaPacienteDto> cuotas, List<PagoDto> pagos) {

    public EstadoCuentaDto {
        cuotas = List.copyOf(cuotas);
        pagos = List.copyOf(pagos);
    }
}
