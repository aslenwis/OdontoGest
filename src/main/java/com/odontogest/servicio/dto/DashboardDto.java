/*
 * OdontoGest - Sistema de gestión de consultorio odontológico con odontograma interactivo
 * Copyright (c) 2026 Apaza Mamani Ruben, Apaza Suca Elwin junior, Inofuente Trujillo David Alex. Todos los derechos reservados.
 * Proyecto del curso de Ingeniería de Software - Universidad Nacional de Juliaca.
 */
package com.odontogest.servicio.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datos del tablero de inicio (RF-REP-01). Para un odontólogo, el monto del mes y la serie son su producción
 * (procedimientos realizados) y las citas de hoy son solo las suyas; para los demás roles son los ingresos de caja.
 *
 * @param esProduccion   {@code true} si {@code montoMes} y {@code ultimosMeses} son producción y no ingresos
 * @param ultimosMeses   seis meses, del más antiguo al actual, incluidos los meses en cero
 * @param morosos        los diez pacientes con mayor deuda vencida (vacío para el odontólogo, que no ve la
 *                       morosidad)
 */
public record DashboardDto(int citasHoy, BigDecimal montoMes, boolean esProduccion, int pacientesNuevosMes,
                           int cuotasVencidas, int pacientesMorosos, BigDecimal saldoVencido,
                           List<MontoPeriodoDto> ultimosMeses, List<CitaDto> agendaHoy, List<MorosoDto> morosos) {

    public DashboardDto {
        ultimosMeses = List.copyOf(ultimosMeses);
        agendaHoy = List.copyOf(agendaHoy);
        morosos = List.copyOf(morosos);
    }
}
