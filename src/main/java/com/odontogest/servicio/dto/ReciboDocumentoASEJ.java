package com.odontogest.servicio.dto;

import java.math.BigDecimal;

public record ReciboDocumentoASEJ(DatosClinicaASEJ clinicaASEJ, PacienteDetalleASEJ pacienteASEJ, PagoDtoASEJ pagoASEJ, BigDecimal saldoPendienteASEJ) {
}
