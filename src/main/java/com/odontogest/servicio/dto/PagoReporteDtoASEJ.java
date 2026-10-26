package com.odontogest.servicio.dto;

import com.odontogest.modelo.MedioPagoASEJ;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoReporteDtoASEJ(LocalDateTime fechaASEJ, String numeroReciboASEJ, String numeroHistoriaASEJ, String pacienteASEJ,
                             MedioPagoASEJ medioASEJ, String referenciaASEJ, BigDecimal montoASEJ) {
}
