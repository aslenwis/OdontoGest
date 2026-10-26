package com.odontogest.servicio.dto;

import com.odontogest.modelo.EstadoAtencionASEJ;

import java.time.LocalDateTime;

public record AtencionResumenASEJ(long idASEJ, LocalDateTime fechaASEJ, String odontologoNombreASEJ, String motivoConsultaASEJ,
                              EstadoAtencionASEJ estadoASEJ) {
}
