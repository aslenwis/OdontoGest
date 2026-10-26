package com.odontogest.servicio.dto;

import java.math.BigDecimal;

public record ProduccionFilaDtoASEJ(long odontologoIdASEJ, String odontologoASEJ, String codigoASEJ, String procedimientoASEJ,
                                int cantidadASEJ, BigDecimal montoASEJ) {
}
