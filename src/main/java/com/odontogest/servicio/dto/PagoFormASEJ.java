package com.odontogest.servicio.dto;

import com.odontogest.modelo.MedioPagoASEJ;

import java.math.BigDecimal;

public record PagoFormASEJ(long pacienteIdASEJ, BigDecimal montoASEJ, MedioPagoASEJ medioASEJ, String referenciaASEJ) {
}
