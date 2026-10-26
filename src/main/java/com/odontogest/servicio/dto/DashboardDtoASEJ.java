package com.odontogest.servicio.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardDtoASEJ(int citasHoyASEJ, BigDecimal montoMesASEJ, boolean esProduccionASEJ, int pacientesNuevosMesASEJ,
                           int cuotasVencidasASEJ, int pacientesMorososASEJ, BigDecimal saldoVencidoASEJ,
                           List<MontoPeriodoDtoASEJ> ultimosMesesASEJ, List<CitaDtoASEJ> agendaHoyASEJ, List<MorosoDtoASEJ> morososASEJ) {

    public DashboardDtoASEJ {
        ultimosMesesASEJ = List.copyOf(ultimosMesesASEJ);
        agendaHoyASEJ = List.copyOf(agendaHoyASEJ);
        morososASEJ = List.copyOf(morososASEJ);
    }
}
