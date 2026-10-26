package com.odontogest.servicio.dto;

import com.odontogest.dominio.odontograma.DiferenciaOdontogramaASEJ;

import java.time.LocalDateTime;

public record DiferenciaVistaASEJ(LocalDateTime baseASEJ, LocalDateTime objetivoASEJ, DiferenciaOdontogramaASEJ diferenciaASEJ) {
}
